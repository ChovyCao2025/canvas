package org.chovy.canvas.engine.disruptor;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.*;
import com.lmax.disruptor.util.DaemonThreadFactory;
import org.chovy.canvas.engine.scheduler.CanvasMetrics;
import org.chovy.canvas.engine.request.CanvasExecutionRequestExecutor;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;

/**
 * Disruptor 分发层（设计文档 12.8节）。
 * 架构：外部触发（MQ / 行为 / 定时）→ Ring Buffer → WorkerPool 消费者 → DagEngine
 * 关键设计选择：
 * - ProducerType.MULTI：多触发源（MQ 消费线程、行为触发、定时调度）并发发布
 * - YieldingWaitStrategy：低延迟，适合画布执行场景
 * - WorkerPool（handleEventsWithWorkerPool）：每个 event 只由一个消费者处理（work-stealing）
 * 而非 handleEventsWith（广播给所有消费者）
 * - Ring Buffer 大小 65536（2^16）≈ 16MB，填满表示积压 65K 条，触发背压
 */
@Slf4j
@Service
public class CanvasDisruptorService {

    /** Disruptor 主实例，负责 worker 生命周期管理。 */
    private final Disruptor<CanvasExecutionEvent> disruptor;

    /** RingBuffer 发布端，供外部触发源写入执行事件。 */
    private final RingBuffer<CanvasExecutionEvent> ringBuffer;
    private final CanvasMetrics metrics;

    // 初始化配置
    public CanvasDisruptorService(
            CanvasExecutionService executionService,
            CanvasExecutionRequestExecutor requestExecutor,
            CanvasMetrics metrics,
            @Value("${canvas.disruptor.ring-buffer-size:65536}") int ringBufferSize,
            @Value("${canvas.disruptor.consumers:0}") int configuredConsumers) {
        this.metrics = metrics;

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
        Arrays.fill(workers, (WorkHandler<CanvasExecutionEvent>) event -> {
            try {
                if (event.requestId != null) {
                    String requestId = event.requestId;
                    requestExecutor.execute(requestId)
                            .subscribe(
                                    null,
                                    e -> log.error("[DISRUPTOR] request 执行失败 requestId={}: {}",
                                            requestId, e.getMessage())
                            );
                } else {
                    executionService.triggerFromDisruptor(
                                    event.canvasId, event.userId, event.triggerType,
                                    event.triggerNodeType, event.matchKey,
                                    event.payload, event.msgId, event.dispatchOptions)
                            .subscribe(
                                    null,
                                    e -> log.error("[DISRUPTOR] 执行失败 canvasId={} userId={}: {}",
                                            event.canvasId, event.userId, e.getMessage())
                            );
                }
            } finally {
                event.reset(); // 归还事件对象（Ring Buffer 复用）
            }
        });
        disruptor.handleEventsWithWorkerPool(workers);

        // 异常处理：记录错误但不中断 Ring Buffer
        disruptor.setDefaultExceptionHandler(new ExceptionHandler<>() {
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
     * 非阻塞，当 Ring Buffer 满时快速失败，调用方可触发上游重试/限流。
     * 仅用于异步触发（MQ / 行为 / 定时），直调触发绕过 Disruptor 直接执行。
     *
     * <p>注意：
     * payload 会复制成新 Map，避免调用方后续修改原对象影响消费侧读取。
     */
    public void publish(Long canvasId, String userId, String triggerType,
                        String triggerNodeType, String matchKey,
                        Map<String, Object> payload, String msgId) {
        publish(canvasId, userId, triggerType, triggerNodeType, matchKey, payload, msgId,
                DispatchOptions.NORMAL);
    }

    /**
     * 发布由溢出重试消费者重放的触发事件。
     * 该标记不来自业务 payload，避免外部调用方伪造重试状态绕过前置检查。
     */
    public void publishOverflowRetry(Long canvasId, String userId, String triggerType,
                                     String triggerNodeType, String matchKey,
                                     Map<String, Object> payload, String msgId) {
        publishOverflowRetry(canvasId, userId, triggerType, triggerNodeType, matchKey, payload, msgId, 0);
    }

    public void publishOverflowRetry(Long canvasId, String userId, String triggerType,
                                     String triggerNodeType, String matchKey,
                                     Map<String, Object> payload, String msgId,
                                     int overflowChainRetryCount) {
        publish(canvasId, userId, triggerType, triggerNodeType, matchKey, payload, msgId,
                DispatchOptions.overflowRetry(overflowChainRetryCount));
    }

    private void publish(Long canvasId, String userId, String triggerType,
                         String triggerNodeType, String matchKey,
                         Map<String, Object> payload, String msgId,
                         DispatchOptions dispatchOptions) {
        long sequence;
        try {
            sequence = ringBuffer.tryNext();
        } catch (InsufficientCapacityException e) {
            metrics.recordDisruptorOverflow(triggerType);
            throw new IllegalStateException("Disruptor Ring Buffer is full", e);
        }
        try {
            CanvasExecutionEvent event = ringBuffer.get(sequence);
            event.canvasId = canvasId;
            event.userId = userId;
            event.triggerType = triggerType;
            event.triggerNodeType = triggerNodeType;
            event.matchKey = matchKey;
            event.payload = payload != null ? new java.util.HashMap<>(payload) : java.util.Map.of();
            event.msgId = msgId;
            event.dispatchOptions = dispatchOptions;
        } finally {
            ringBuffer.publish(sequence);
        }
        metrics.recordDisruptorPublished(triggerType);
    }

    public void publishRequest(String requestId) {
        long sequence;
        try {
            sequence = ringBuffer.tryNext();
        } catch (InsufficientCapacityException e) {
            metrics.recordDisruptorOverflow("REQUEST");
            throw new IllegalStateException("Disruptor Ring Buffer is full", e);
        }
        try {
            CanvasExecutionEvent event = ringBuffer.get(sequence);
            event.requestId = requestId;
        } finally {
            ringBuffer.publish(sequence);
        }
        metrics.recordDisruptorPublished("REQUEST");
    }

    @PreDestroy
    public void shutdown() {
        disruptor.shutdown();
        log.info("[DISRUPTOR] 已关闭");
    }

    /**
     * Disruptor-only dispatch metadata. Instances are not publicly constructible,
     * so external trigger payloads cannot opt into internal replay behavior.
     */
    public static final class DispatchOptions {
        private static final DispatchOptions NORMAL = new DispatchOptions(false);

        private final boolean overflowRetry;
        private final int overflowChainRetryCount;

        private DispatchOptions(boolean overflowRetry) {
            this(overflowRetry, 0);
        }

        private DispatchOptions(boolean overflowRetry, int overflowChainRetryCount) {
            this.overflowRetry = overflowRetry;
            this.overflowChainRetryCount = Math.max(0, overflowChainRetryCount);
        }

        static DispatchOptions overflowRetry(int overflowChainRetryCount) {
            return new DispatchOptions(true, overflowChainRetryCount);
        }

        public boolean isOverflowRetry() {
            return overflowRetry;
        }

        public int getOverflowChainRetryCount() {
            return overflowChainRetryCount;
        }
    }
}
