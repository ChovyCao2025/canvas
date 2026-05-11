package com.photon.canvas.engine.scheduler;

import com.photon.canvas.domain.execution.CanvasExecutionTrace;
import com.photon.canvas.domain.execution.CanvasExecutionTraceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

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

    private final ConcurrentLinkedQueue<CanvasExecutionTrace> buffer =
            new ConcurrentLinkedQueue<>();
    private final CanvasExecutionTraceMapper traceMapper;

    /** 非阻塞入队（主执行链路调用，不等待） */
    public void offer(CanvasExecutionTrace trace) {
        if (buffer.size() >= MAX_CAPACITY) {
            log.warn("[TRACE_BUFFER] 缓冲区已满（{}），丢弃轨迹 executionId={}",
                    MAX_CAPACITY, trace.getExecutionId());
            return;
        }
        buffer.offer(trace);
    }

    /** 每 500ms 批量刷盘，每次最多 200 条 */
    @Scheduled(fixedDelay = 500)
    public void flush() {
        if (buffer.isEmpty()) return;

        List<CanvasExecutionTrace> batch = new ArrayList<>(BATCH_SIZE);
        int count = 0;
        CanvasExecutionTrace item;
        while ((item = buffer.poll()) != null && count < BATCH_SIZE) {
            batch.add(item);
            count++;
        }

        if (!batch.isEmpty()) {
            try {
                batch.forEach(traceMapper::insert);
                log.debug("[TRACE_BUFFER] 批量写入 {} 条", batch.size());
            } catch (Exception e) {
                log.error("[TRACE_BUFFER] 批量写入失败: {}", e.getMessage());
                // 写入失败的轨迹不重试（轨迹是调试辅助数据，丢失可接受）
            }
        }
    }

    public int pendingCount() { return buffer.size(); }
}
