package org.chovy.canvas.engine.scheduler;

import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionTraceMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

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
@RequiredArgsConstructor
public class TraceWriteBuffer {

    private static final int BATCH_SIZE   = 200;
    private static final int MAX_CAPACITY = 50_000;
    private static final int MAX_BATCHES_PER_FLUSH = 20;

    private final ConcurrentLinkedQueue<CanvasExecutionTraceDO> buffer =
            new ConcurrentLinkedQueue<>();
    private final CanvasExecutionTraceMapper traceMapper;
    private final AtomicInteger pending = new AtomicInteger(0);

    /** 非阻塞入队（主执行链路调用，不等待） */
    public void offer(CanvasExecutionTraceDO trace) {
        int current = pending.incrementAndGet();
        if (current > MAX_CAPACITY) {
            pending.decrementAndGet();
            log.warn("[TRACE_BUFFER] 缓冲区已满（{}），丢弃轨迹 executionId={}",
                    MAX_CAPACITY, trace.getExecutionId());
            return;
        }
        buffer.offer(trace);
    }

    /** 每 500ms 批量刷盘，每次最多 200 条 */
    @Scheduled(fixedDelay = 500)
    public void flush() {
        if (pending.get() <= 0) return;

        for (int i = 0; i < MAX_BATCHES_PER_FLUSH; i++) {
            List<CanvasExecutionTraceDO> batch = drainBatch();
            if (batch.isEmpty()) return;
            writeBatch(batch);
        }
    }

    /** 应用关闭前将缓冲区剩余轨迹全部刷盘，防止丢失 */
    @PreDestroy
    public void shutdownFlush() {
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

    private List<CanvasExecutionTraceDO> drainBatch() {
        List<CanvasExecutionTraceDO> batch = new ArrayList<>(BATCH_SIZE);
        CanvasExecutionTraceDO item;
        while ((item = buffer.poll()) != null && batch.size() < BATCH_SIZE) {
            batch.add(item);
        }
        if (!batch.isEmpty()) {
            pending.addAndGet(-batch.size());
        }
        return batch;
    }

    private void writeBatch(List<CanvasExecutionTraceDO> batch) {
        try {
            traceMapper.insertBatch(batch);
            log.debug("[TRACE_BUFFER] 批量写入 {} 条", batch.size());
        } catch (Exception e) {
            log.error("[TRACE_BUFFER] 批量写入失败: {}", e.getMessage());
        }
    }

    public int pendingCount() { return pending.get(); }
}
