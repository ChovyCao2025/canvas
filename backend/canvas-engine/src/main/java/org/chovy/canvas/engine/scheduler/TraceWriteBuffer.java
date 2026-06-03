package org.chovy.canvas.engine.scheduler;

import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionTraceMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 执行轨迹异步批量写入缓冲区（设计文档 12.10节）。
 *
 * 主执行链路调用 offer() 非阻塞（<1μs），不等待 DB 写入。
 * 后台线程每 500ms 批量 INSERT，降低主链路延迟 5~20ms。
 *
 * 背压保护：队列上限 50000 条，非关键轨迹高水位采样降级，关键轨迹失败时显式抛错。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TraceWriteBuffer {

    /** 单批轨迹刷盘条数。 */
    private static final int BATCH_SIZE   = 200;
    /** 内存缓冲区最大容量。 */
    private static final int MAX_CAPACITY = 50_000;
    /** 单次定时 flush 最多处理的批次数。 */
    private static final int MAX_BATCHES_PER_FLUSH = 20;
    /** 非关键轨迹进入降级采样的水位线。 */
    private static final int NON_CRITICAL_SAMPLING_THRESHOLD = MAX_CAPACITY * 80 / 100;
    /** 高水位后非关键轨迹保留比例：每 10 条保留 1 条。 */
    private static final int NON_CRITICAL_SAMPLE_RATE = 10;
    /** 关键轨迹入队前最多尝试主动 flush 的次数。 */
    private static final int CRITICAL_ENQUEUE_ATTEMPTS = 3;
    /** 定时刷盘线程编号。 */
    private static final AtomicInteger THREAD_SEQUENCE = new AtomicInteger(1);

    /** 待批量写入的执行轨迹内存队列。 */
    private final ConcurrentLinkedQueue<CanvasExecutionTraceDO> buffer =
            new ConcurrentLinkedQueue<>();
    /** 执行轨迹 Mapper。 */
    private final CanvasExecutionTraceMapper traceMapper;
    /** 当前待刷盘轨迹数量。 */
    private final AtomicInteger pending = new AtomicInteger(0);
    /** 高水位非关键轨迹采样计数器。 */
    private final AtomicInteger nonCriticalSampleCounter = new AtomicInteger(0);
    /** 防止 Spring lifecycle 或测试重复启动定时线程。 */
    private final AtomicBoolean schedulerStarted = new AtomicBoolean(false);
    /** 独立于 Spring scheduler 的刷盘线程池。 */
    private volatile ScheduledExecutorService flushExecutor;
    /** 测试钩子：观察定时刷盘线程，不参与业务逻辑。 */
    private volatile Consumer<Thread> scheduledFlushCallback;

    /** 非阻塞入队（主执行链路调用，不等待） */
    public void offer(CanvasExecutionTraceDO trace) {
        addTrace(trace, false);
    }

    /**
     * 入队执行轨迹。
     *
     * @param trace 执行轨迹
     * @param critical 是否关键轨迹；关键轨迹不会静默丢弃
     * @return 是否成功入队
     */
    public boolean addTrace(CanvasExecutionTraceDO trace, boolean critical) {
        if (trace == null) {
            throw new IllegalArgumentException("trace must not be null");
        }

        if (critical) {
            return addCriticalTrace(trace);
        }
        return addNonCriticalTrace(trace);
    }

    /** bean 初始化后启动独立刷盘线程。 */
    @PostConstruct
    void startScheduler() {
        if (!schedulerStarted.compareAndSet(false, true)) {
            return;
        }
        flushExecutor = Executors.newSingleThreadScheduledExecutor(traceFlushThreadFactory());
        flushExecutor.scheduleWithFixedDelay(this::scheduledFlush, 0, 500, TimeUnit.MILLISECONDS);
    }

    /** 每 500ms 批量刷盘，每批最多 200 条 */
    public void flush() {
        if (pending.get() <= 0) return;

        for (int i = 0; i < MAX_BATCHES_PER_FLUSH; i++) {
            List<CanvasExecutionTraceDO> batch = drainBatch();
            if (batch.isEmpty()) return;
            // 单次调度最多刷 MAX_BATCHES_PER_FLUSH 批，避免 flush 长时间占用调度线程。
            writeBatch(batch);
        }
    }

    /** 应用关闭前将缓冲区剩余轨迹全部刷盘，防止丢失 */
    @PreDestroy
    public void shutdownFlush() {
        shutdownScheduler();
        int remaining = pending.get();
        if (remaining == 0) return;
        log.info("[TRACE_BUFFER] 关闭前刷盘，剩余 {} 条", remaining);
        // 循环直到清空，不受 BATCH_SIZE 限制
        List<CanvasExecutionTraceDO> batch = new ArrayList<>(BATCH_SIZE);
        CanvasExecutionTraceDO item;
        while ((item = buffer.poll()) != null) {
            batch.add(item);
            pending.decrementAndGet();
            if (batch.size() >= BATCH_SIZE) {
                writeBatch(batch);
                batch = new ArrayList<>(BATCH_SIZE);
            }
        }
        if (!batch.isEmpty()) writeBatch(batch);
        log.info("[TRACE_BUFFER] 关闭刷盘完成");
    }

    void onScheduledFlushForTest(Consumer<Thread> callback) {
        this.scheduledFlushCallback = callback;
    }

    private boolean addNonCriticalTrace(CanvasExecutionTraceDO trace) {
        int current = pending.get();
        if (current >= MAX_CAPACITY) {
            log.warn("[TRACE_BUFFER] 缓冲区已满（{}），降级丢弃非关键轨迹 executionId={}",
                    MAX_CAPACITY, trace.getExecutionId());
            return false;
        }
        if (current >= NON_CRITICAL_SAMPLING_THRESHOLD && shouldDropNonCriticalSample(trace)) {
            return false;
        }
        if (tryEnqueue(trace)) {
            return true;
        }
        log.warn("[TRACE_BUFFER] 缓冲区已满（{}），降级丢弃非关键轨迹 executionId={}",
                MAX_CAPACITY, trace.getExecutionId());
        return false;
    }

    private boolean addCriticalTrace(CanvasExecutionTraceDO trace) {
        for (int i = 0; i < CRITICAL_ENQUEUE_ATTEMPTS; i++) {
            if (tryEnqueue(trace)) {
                return true;
            }
            flush();
            if (tryEnqueue(trace)) {
                return true;
            }
            briefBackoff();
        }
        throw new IllegalStateException("[TRACE_BUFFER] critical trace could not be enqueued after flush attempts; executionId="
                + trace.getExecutionId());
    }

    private boolean tryEnqueue(CanvasExecutionTraceDO trace) {
        while (true) {
            int current = pending.get();
            if (current >= MAX_CAPACITY) {
                return false;
            }
            if (pending.compareAndSet(current, current + 1)) {
                // 只入内存队列，真正 DB 写入由后台 flush 批量完成。
                buffer.offer(trace);
                return true;
            }
        }
    }

    private boolean shouldDropNonCriticalSample(CanvasExecutionTraceDO trace) {
        int sample = nonCriticalSampleCounter.incrementAndGet();
        boolean drop = sample % NON_CRITICAL_SAMPLE_RATE != 0;
        if (drop) {
            if (sample % 1_000 == 1) {
                log.warn("[TRACE_BUFFER] 缓冲区高水位（pending={}），采样丢弃非关键轨迹 executionId={}",
                        pending.get(), trace.getExecutionId());
            } else {
                log.debug("[TRACE_BUFFER] 缓冲区高水位（pending={}），采样丢弃非关键轨迹 executionId={}",
                        pending.get(), trace.getExecutionId());
            }
        }
        return drop;
    }

    private void scheduledFlush() {
        try {
            Consumer<Thread> callback = scheduledFlushCallback;
            if (callback != null) {
                callback.accept(Thread.currentThread());
            }
            flush();
        } catch (Exception e) {
            log.error("[TRACE_BUFFER] 定时刷盘异常: {}", e.getMessage(), e);
        }
    }

    private ThreadFactory traceFlushThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable,
                    "trace-write-buffer-flush-" + THREAD_SEQUENCE.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }

    private void shutdownScheduler() {
        ScheduledExecutorService executor = flushExecutor;
        if (executor == null) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        } finally {
            flushExecutor = null;
            schedulerStarted.set(false);
        }
    }

    private void briefBackoff() {
        try {
            TimeUnit.MILLISECONDS.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("[TRACE_BUFFER] interrupted while waiting to enqueue critical trace", e);
        }
    }

    /**
     * 执行 drain Batch 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 查询、转换或计算得到的结果集合
     */
    private List<CanvasExecutionTraceDO> drainBatch() {
        List<CanvasExecutionTraceDO> batch = new ArrayList<>(BATCH_SIZE);
        CanvasExecutionTraceDO item;
        while (batch.size() < BATCH_SIZE && (item = buffer.poll()) != null) {
            batch.add(item);
        }
        if (!batch.isEmpty()) {
            // pending 与队列 poll 成功数量保持一致，作为 backlog gauge 的数据源。
            pending.addAndGet(-batch.size());
        }
        return batch;
    }

    /**
     * 写入或记录 write Batch 相关的业务数据。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param batch batch 方法执行所需的业务参数
     */
    private void writeBatch(List<CanvasExecutionTraceDO> batch) {
        try {
            traceMapper.insertBatch(batch);
            log.debug("[TRACE_BUFFER] 批量写入 {} 条", batch.size());
        } catch (Exception e) {
            // trace 是旁路审计数据，写失败只记录日志，不反向影响节点执行结果。
            log.error("[TRACE_BUFFER] 批量写入失败: {}", e.getMessage());
        }
    }

    /**
     * 执行 pending Count 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 计算得到的数值结果
     */
    public int pendingCount() { return pending.get(); }
}
