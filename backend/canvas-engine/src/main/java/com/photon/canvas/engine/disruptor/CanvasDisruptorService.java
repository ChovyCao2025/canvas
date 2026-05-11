package com.photon.canvas.engine.disruptor;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.*;
import com.photon.canvas.engine.trigger.CanvasExecutionService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Disruptor 分发层（设计文档 12.8节）。
 *
 * 架构：外部触发（MQ / 行为 / 定时）→ Ring Buffer → WorkerPool 消费者 → DagEngine
 *
 * 关键设计选择：
 * - ProducerType.MULTI：多触发源（MQ 消费线程、行为触发、定时调度）并发发布
 * - YieldingWaitStrategy：低延迟，适合画布执行场景
 * - WorkerPool（handleEventsWithWorkerPool）：每个 event 只由一个消费者处理（work-stealing）
 *   而非 handleEventsWith（广播给所有消费者）
 * - Ring Buffer 大小 65536（2^16）≈ 16MB，填满表示积压 65K 条，触发背压
 */
@Slf4j
@Service
public class CanvasDisruptorService {

    private final Disruptor<CanvasExecutionEvent> disruptor;
    private final RingBuffer<CanvasExecutionEvent> ringBuffer;

    public CanvasDisruptorService(
            CanvasExecutionService executionService,
            @Value("${canvas.disruptor.ring-buffer-size:65536}") int ringBufferSize,
            @Value("${canvas.disruptor.consumers:0}") int configuredConsumers) {

        // consumers=0 时自动使用 CPU 核数
        int consumers = configuredConsumers > 0
                ? configuredConsumers
                : Runtime.getRuntime().availableProcessors();

        disruptor = new Disruptor<>(
                CanvasExecutionEvent::new,
                ringBufferSize,
                DaemonThreadFactory.INSTANCE,
                ProducerType.MULTI,         // 多触发源
                new YieldingWaitStrategy()   // 低延迟（设计文档推荐）
        );

        // WorkerPool：每个 event 只由一个 worker 处理
        @SuppressWarnings("unchecked")
        WorkHandler<CanvasExecutionEvent>[] workers = new WorkHandler[consumers];
        for (int i = 0; i < consumers; i++) {
            workers[i] = event -> {
                try {
                    executionService.trigger(
                                    event.canvasId, event.userId, event.triggerType,
                                    event.triggerNodeType, event.matchKey,
                                    event.payload, event.msgId, false)
                            .subscribe(
                                    null,
                                    e -> log.error("[DISRUPTOR] 执行失败 canvasId={} userId={}: {}",
                                            event.canvasId, event.userId, e.getMessage())
                            );
                } finally {
                    event.reset(); // 归还事件对象（Ring Buffer 复用）
                }
            };
        }
        disruptor.handleEventsWithWorkerPool(workers);

        // 异常处理：记录错误但不中断 Ring Buffer
        disruptor.setDefaultExceptionHandler(new ExceptionHandler<CanvasExecutionEvent>() {
            @Override
            public void handleEventException(Throwable ex, long seq, CanvasExecutionEvent e) {
                log.error("[DISRUPTOR] event 处理异常 seq={} canvasId={}: {}",
                        seq, e.canvasId, ex.getMessage());
            }
            @Override
            public void handleOnStartException(Throwable ex) {
                log.error("[DISRUPTOR] 启动异常: {}", ex.getMessage());
            }
            @Override
            public void handleOnShutdownException(Throwable ex) {
                log.error("[DISRUPTOR] 关闭异常: {}", ex.getMessage());
            }
        });

        ringBuffer = disruptor.start();
        log.info("[DISRUPTOR] 启动 ringBufferSize={} consumers={}", ringBufferSize, consumers);
    }

    /**
     * 发布触发事件到 Ring Buffer（设计文档 12.8节）。
     * 非阻塞，当 Ring Buffer 满时 next() 自旋等待（YieldingWaitStrategy）。
     *
     * 仅用于异步触发（MQ / 行为 / 定时），直调触发绕过 Disruptor 直接执行。
     */
    public void publish(Long canvasId, String userId, String triggerType,
                         String triggerNodeType, String matchKey,
                         Map<String, Object> payload, String msgId) {
        long sequence = ringBuffer.next();
        try {
            CanvasExecutionEvent event = ringBuffer.get(sequence);
            event.canvasId        = canvasId;
            event.userId          = userId;
            event.triggerType     = triggerType;
            event.triggerNodeType = triggerNodeType;
            event.matchKey        = matchKey;
            event.payload         = payload != null ? new java.util.HashMap<>(payload) : java.util.Map.of();
            event.msgId           = msgId;
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    /** 剩余可用容量（用于监控背压） */
    public long remainingCapacity() { return ringBuffer.remainingCapacity(); }

    @PreDestroy
    public void shutdown() {
        disruptor.shutdown();
        log.info("[DISRUPTOR] 已关闭");
    }
}
