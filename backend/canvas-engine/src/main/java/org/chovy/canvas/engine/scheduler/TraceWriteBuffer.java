package org.chovy.canvas.engine.scheduler;

import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
import org.chovy.canvas.dal.mapper.AnalyticsEventMapper;
import org.chovy.canvas.dal.mapper.AnalyticsEventTraceMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionTraceMapper;
import org.chovy.canvas.domain.analytics.MySqlTraceEventSink;
import org.chovy.canvas.domain.analytics.TraceEventSink;
import org.chovy.canvas.infrastructure.doris.DorisStreamLoader;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 执行轨迹异步批量写入缓冲区（设计文档 12.10节）。
 *
 * 主执行链路调用 offer() 非阻塞（<1μs），不等待 DB 写入。
 * 后台线程每 500ms 或积满 200 条时批量 INSERT，降低主链路延迟 5~20ms。
 *
 * 背压保护：队列上限 50000 条，超出时丢弃并告警。
 */
@Slf4j
@Component
public class TraceWriteBuffer {

    /** 单批轨迹刷盘条数。 */
    private static final int BATCH_SIZE   = 200;
    /** 内存缓冲区最大容量。 */
    private static final int MAX_CAPACITY = 50_000;
    /** 单次定时 flush 最多处理的批次数。 */
    private static final int MAX_BATCHES_PER_FLUSH = 20;

    /** 待批量写入的执行轨迹内存队列。 */
    private final ConcurrentLinkedQueue<CanvasExecutionTraceDO> buffer =
            new ConcurrentLinkedQueue<>();
    /** OLAP-ready trace/event sink, keeping MySQL compatibility behind one boundary. */
    private final TraceEventSink sink;
    /** Doris Stream Load 写入器；迁移期与 MySQL 双写，禁用时为空操作。 */
    private final DorisStreamLoader dorisStreamLoader;
    /** 内存缓冲区最大容量。 */
    private final int maxCapacity;
    /** 单批轨迹刷盘条数。 */
    private final int batchSize;
    /** 当前待刷盘轨迹数量。 */
    private final AtomicInteger pending = new AtomicInteger(0);
    /** 因背压丢弃的轨迹数量。 */
    private final AtomicLong droppedCount = new AtomicLong(0);
    /** 最近一次 flush 耗时。 */
    private final AtomicLong lastFlushDurationMs = new AtomicLong(0);

    /**
     * 创建 TraceWriteBuffer 实例并注入 engine.scheduler 场景依赖。
     * @param traceMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public TraceWriteBuffer(CanvasExecutionTraceMapper traceMapper) {
        this(traceMapper, null, null, null);
    }

    /**
     * 创建 TraceWriteBuffer 实例并注入 engine.scheduler 场景依赖。
     * @param traceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dorisStreamLoader doris stream loader 参数，用于 TraceWriteBuffer 流程中的校验、计算或对象转换。
     */
    @Autowired
    public TraceWriteBuffer(CanvasExecutionTraceMapper traceMapper,
                            DorisStreamLoader dorisStreamLoader,
                            AnalyticsEventTraceMapper analyticsTraceMapper,
                            AnalyticsEventMapper analyticsEventMapper) {
        this(new MySqlTraceEventSink(traceMapper, analyticsTraceMapper, analyticsEventMapper),
                dorisStreamLoader, MAX_CAPACITY, BATCH_SIZE);
    }

    public TraceWriteBuffer(CanvasExecutionTraceMapper traceMapper, DorisStreamLoader dorisStreamLoader) {
        this(traceMapper, dorisStreamLoader, null, null);
    }

    TraceWriteBuffer(CanvasExecutionTraceMapper traceMapper,
                     DorisStreamLoader dorisStreamLoader,
                     int maxCapacity,
                     int batchSize) {
        this(new MySqlTraceEventSink(traceMapper, null, null), dorisStreamLoader, maxCapacity, batchSize);
    }

    TraceWriteBuffer(TraceEventSink sink, int maxCapacity, int batchSize) {
        this(sink, null, maxCapacity, batchSize);
    }

    private TraceWriteBuffer(TraceEventSink sink,
                             DorisStreamLoader dorisStreamLoader,
                             int maxCapacity,
                             int batchSize) {
        this.sink = sink;
        this.dorisStreamLoader = dorisStreamLoader;
        this.maxCapacity = maxCapacity;
        this.batchSize = batchSize;
    }

    /** 非阻塞入队（主执行链路调用，不等待） */
    public void offer(CanvasExecutionTraceDO trace) {
        int current = pending.incrementAndGet();
        if (current > maxCapacity) {
            // 背压保护：宁可丢 trace，也不能让主执行链路被轨迹写入拖垮。
            pending.decrementAndGet();
            droppedCount.incrementAndGet();
            log.warn("[TRACE_BUFFER] 缓冲区已满（{}），丢弃轨迹 executionId={}",
                    maxCapacity, trace.getExecutionId());
            return;
        }
        // 只入内存队列，真正 DB 写入由定时 flush 批量完成。
        buffer.offer(trace);
    }

    /** 每 500ms 批量刷盘，每次最多 200 条 */
    @Scheduled(fixedDelay = 500)
    public void flush() {
        if (pending.get() <= 0) return;
        long startNanos = System.nanoTime();

        try {
            for (int i = 0; i < MAX_BATCHES_PER_FLUSH; i++) {
                List<CanvasExecutionTraceDO> batch = drainBatch();
                if (batch.isEmpty()) return;
                // 单次调度最多刷 MAX_BATCHES_PER_FLUSH 批，避免 flush 长时间占用调度线程。
                writeBatch(batch);
            }
        } finally {
            lastFlushDurationMs.set((System.nanoTime() - startNanos) / 1_000_000L);
        }
    }

    /** 应用关闭前将缓冲区剩余轨迹全部刷盘，防止丢失 */
    @PreDestroy
    public void shutdownFlush() {
        int remaining = pending.get();
        if (remaining == 0) return;
        log.info("[TRACE_BUFFER] 关闭前刷盘，剩余 {} 条", remaining);
        // 循环直到清空，不受 BATCH_SIZE 限制
        List<CanvasExecutionTraceDO> batch = new ArrayList<>(batchSize);
        CanvasExecutionTraceDO item;
        while ((item = buffer.poll()) != null) {
            batch.add(item);
            pending.decrementAndGet();
            if (batch.size() >= batchSize) {
                writeBatch(batch);
                batch = new ArrayList<>(batchSize);
            }
        }
        if (!batch.isEmpty()) writeBatch(batch);
        log.info("[TRACE_BUFFER] 关闭刷盘完成");
    }

    /**
     * 执行 drain Batch 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 查询、转换或计算得到的结果集合
     */
    private List<CanvasExecutionTraceDO> drainBatch() {
        List<CanvasExecutionTraceDO> batch = new ArrayList<>(batchSize);
        CanvasExecutionTraceDO item;
        while ((item = buffer.poll()) != null && batch.size() < batchSize) {
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
        if (batch == null || batch.isEmpty()) {
            return;
        }
        try {
            sink.writeTraces(batch);
            log.debug("[TRACE_BUFFER] sink 批量写入 {} 条", batch.size());
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            // trace 是旁路审计数据，写失败只记录日志，不反向影响节点执行结果。
            log.error("[TRACE_BUFFER] sink 批量写入失败: {}", e.getMessage());
        }

        try {
            if (dorisStreamLoader != null) {
                dorisStreamLoader.load(batch);
            }
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            // Doris 是迁移期 OLAP 旁路，失败不能反向影响主链路或 MySQL fallback。
            log.warn("[TRACE_BUFFER] Doris Stream Load 失败: {}", e.getMessage());
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

    public TraceBufferMetrics metrics() {
        TraceEventSink.SinkMetrics sinkMetrics = sink.metrics();
        return new TraceBufferMetrics(
                sinkMetrics.writtenCount(),
                sinkMetrics.failedCount(),
                droppedCount.get() + sinkMetrics.droppedCount(),
                pending.get(),
                lastFlushDurationMs.get());
    }

    public record TraceBufferMetrics(
            long writtenCount,
            long failedCount,
            long droppedCount,
            long pendingCount,
            long lastFlushDurationMs) {

        public long backlog() {
            return pendingCount;
        }
    }
}
