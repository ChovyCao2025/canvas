package org.chovy.canvas.engine.disruptor;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.*;
import com.lmax.disruptor.util.DaemonThreadFactory;
import lombok.Getter;
import org.chovy.canvas.engine.scheduler.CanvasMetrics;
import org.chovy.canvas.engine.request.CanvasExecutionRequestExecutor;
import org.chovy.canvas.engine.lane.ExecutionLane;
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

    /**
     * Disruptor 主实例，负责 worker 生命周期管理。
     */
    private final Disruptor<CanvasExecutionEvent> disruptor;

    /**
     * RingBuffer 发布端，供外部触发源写入执行事件。
     */
    private final RingBuffer<CanvasExecutionEvent> ringBuffer;
    /** 画布执行指标组件。 */
    private final CanvasMetrics metrics;

    /** 初始化 Disruptor、worker pool 和异常处理器。 */
    public CanvasDisruptorService(
            CanvasExecutionService executionService,
            CanvasExecutionRequestExecutor requestExecutor,
            CanvasMetrics metrics,
            @Value("${canvas.disruptor.ring-buffer-size:65536}") int ringBufferSize,
            @Value("${canvas.disruptor.consumers:0}") int configuredConsumers
    ) {
        this.metrics = metrics;

        // consumers=0 时自动使用 CPU 核数
        int consumers = determineConsumerCount(configuredConsumers);

        // 创建 Disruptor 实例
        disruptor = createDisruptor(ringBufferSize);

        // WorkerPool：每个 event 只由一个 worker 处理
        // >>> 配置具体的 worker 处理逻辑
        configureWorkerPool(executionService, requestExecutor, consumers);

        // 配置异常处理器
        configureExceptionHandler();

        // 启动 Disruptor 并返回 RingBuffer
        ringBuffer = startDisruptor(ringBufferSize, consumers);
    }

    /** 启动 Disruptor 并返回对外发布事件的 RingBuffer。 */
    private RingBuffer<CanvasExecutionEvent> startDisruptor(int ringBufferSize, int consumers) {
        final RingBuffer<CanvasExecutionEvent> ringBuffer;
        ringBuffer = disruptor.start();
        log.info("[DISRUPTOR] 启动 ringBufferSize={} consumers={}", ringBufferSize, consumers);
        return ringBuffer;
    }

    /** 配置默认异常处理器，确保单个事件异常不会中断消费线程。 */
    private void configureExceptionHandler() {
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
    }

    /** 按消费者数量创建 WorkerPool，使每个事件只被一个 worker 处理。 */
    private void configureWorkerPool(CanvasExecutionService executionService, CanvasExecutionRequestExecutor requestExecutor, int consumers) {
        @SuppressWarnings("unchecked")
        WorkHandler<CanvasExecutionEvent>[] workers = new WorkHandler[consumers];
        // >>> 配置具体的 worker 处理逻辑
        Arrays.fill(workers, createWorkerHandler(executionService, requestExecutor));
        // WorkerPool 只会让一个 worker 拿到同一个 event，适合执行请求这种单消费语义。
        disruptor.handleEventsWithWorkerPool(workers);
    }

    /** 创建根据事件类型分派到画布执行或请求执行器的 worker。 */
    private static WorkHandler<CanvasExecutionEvent> createWorkerHandler(CanvasExecutionService executionService, CanvasExecutionRequestExecutor requestExecutor) {
        return event -> {
            try {
                if (event.requestId != null) {
                    // 处理请求类型事件
                    handleRequestEvent(requestExecutor, event);
                } else {
                    // 处理画布执行类型事件
                    handleCanvasEvent(executionService, event);
                }
            } finally {
                // 复用前必须清空字段，否则下一次 ring buffer 取到的内容会串链路。
                event.reset(); // 归还事件对象（Ring Buffer 复用）
            }
        };
    }

    /** 处理直接触发画布执行的 Disruptor 事件。 */
    private static void handleCanvasEvent(CanvasExecutionService executionService, CanvasExecutionEvent event) {
        // 直接触发链路不落库，事件消费后立刻进入 DAG 执行。
        executionService.triggerFromDisruptor(
                        event.canvasId, event.userId, event.triggerType,
                        event.triggerNodeType, event.matchKey,
                        event.payload, event.msgId, event.executionLane, event.dispatchOptions)
                .subscribe(
                        null,
                        e -> log.error("[DISRUPTOR] 执行失败 canvasId={} userId={}: {}",
                                event.canvasId, event.userId, e.getMessage())
                );
    }

    /** 处理已入库执行请求的 Disruptor 事件。 */
    private static void handleRequestEvent(CanvasExecutionRequestExecutor requestExecutor, CanvasExecutionEvent event) {
        String requestId = event.requestId;
        // 已入库请求由 executor 接管状态迁移和失败重试，不在这里重复实现。
        requestExecutor.execute(requestId)
                .subscribe(
                        null,
                        e -> log.error("[DISRUPTOR] request 执行失败 requestId={}: {}",
                                requestId, e.getMessage())
                );
    }

    /**
     * 创建 Disruptor 实例
     */
    private static Disruptor<CanvasExecutionEvent> createDisruptor(int ringBufferSize) {
        return new Disruptor<>(
                CanvasExecutionEvent::new,
                ringBufferSize,
                DaemonThreadFactory.INSTANCE,
                ProducerType.MULTI,         // 多触发源
                new YieldingWaitStrategy()   // 低延迟（设计文档推荐）
        );
    }

    /**
     * 确定消费者数量：0 时自动使用 CPU 核数
     */
    private static int determineConsumerCount(int configuredConsumers) {
        return configuredConsumers > 0
                ? configuredConsumers
                : Runtime.getRuntime().availableProcessors();
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
                null, DispatchOptions.NORMAL);
    }

    /** 发布带预解析 lane metadata 的触发事件。 */
    public void publish(Long canvasId, String userId, String triggerType,
                        String triggerNodeType, String matchKey,
                        Map<String, Object> payload, String msgId,
                        ExecutionLane executionLane) {
        publish(canvasId, userId, triggerType, triggerNodeType, matchKey, payload, msgId,
                executionLane, DispatchOptions.NORMAL);
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

    /** 发布来自溢出重试链路的执行事件。 */
    public void publishOverflowRetry(Long canvasId, String userId, String triggerType,
                                     String triggerNodeType, String matchKey,
                                     Map<String, Object> payload, String msgId,
                                     int overflowChainRetryCount) {
        publish(canvasId, userId, triggerType, triggerNodeType, matchKey, payload, msgId,
                ExecutionLane.RETRY, DispatchOptions.overflowRetry(overflowChainRetryCount));
    }

    /** 写入 RingBuffer 事件槽位并发布到 worker pool。 */
    private void publish(Long canvasId, String userId, String triggerType,
                         String triggerNodeType, String matchKey,
                         Map<String, Object> payload, String msgId,
                         ExecutionLane executionLane,
                         DispatchOptions dispatchOptions) {
        long sequence;
        try {
            // tryNext 失败说明 ring buffer 已满，属于上游回压信号。
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
            event.executionLane = executionLane;
            event.dispatchOptions = dispatchOptions;
        } finally {
            // >>> 事件处理逻辑见: org.chovy.canvas.engine.disruptor.CanvasDisruptorService.CanvasDisruptorService
            ringBuffer.publish(sequence);
        }
        // 成功发布后再记一次指标，便于区分吞吐和溢出。
        metrics.recordDisruptorPublished(triggerType);
    }

    /** 发布已入库的执行请求 ID 事件。 */
    public void publishRequest(String requestId) {
        long sequence;
        try {
            // 请求事件和普通触发事件共享同一个 ring buffer，满时同样需要快速失败。
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

    /** 关闭服务时释放订阅和连接资源。 */
    @PreDestroy
    public void shutdown() {
        disruptor.shutdown();
        log.info("[DISRUPTOR] 已关闭");
    }

    /**
     * Disruptor-only dispatch metadata. Instances are not publicly constructible,
     * so external trigger payloads cannot opt into internal replay behavior.
     */
    @Getter
    public static final class DispatchOptions {
        /** 普通触发事件的默认派发选项，不携带溢出重试标记。 */
        private static final DispatchOptions NORMAL = new DispatchOptions(false);

        /** 是否来自 Disruptor 溢出重试队列。 */
        private final boolean overflowRetry;
        /** 溢出重试链路已经重试的次数。 */
        private final int overflowChainRetryCount;

        /**
         * 构造 DispatchOptions 实例，并根据入参初始化依赖、配置或内部状态。
         *
         * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
         *
         * @param overflowRetry overflowRetry 方法执行所需的业务参数
         */
        private DispatchOptions(boolean overflowRetry) {
            this(overflowRetry, 0);
        }

        /**
         * 构造 DispatchOptions 实例，并根据入参初始化依赖、配置或内部状态。
         *
         * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
         *
         * @param overflowRetry overflowRetry 方法执行所需的业务参数
         * @param overflowChainRetryCount overflowChainRetryCount 数量、阈值或分页参数
         */
        private DispatchOptions(boolean overflowRetry, int overflowChainRetryCount) {
            this.overflowRetry = overflowRetry;
            this.overflowChainRetryCount = Math.max(0, overflowChainRetryCount);
        }

        /**
         * 执行 overflow Retry 对应的业务逻辑。
         *
         * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
         *
         * @param overflowChainRetryCount overflowChainRetryCount 数量、阈值或分页参数
         * @return 当前对象实例，便于继续链式配置或后续处理
         */
        static DispatchOptions overflowRetry(int overflowChainRetryCount) {
            return new DispatchOptions(true, overflowChainRetryCount);
        }

    }
}
