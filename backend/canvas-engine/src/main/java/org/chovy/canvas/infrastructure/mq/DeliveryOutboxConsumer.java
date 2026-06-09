package org.chovy.canvas.infrastructure.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.engine.delivery.DeliveryOutboxDO;
import org.chovy.canvas.engine.delivery.DeliveryOutboxService;
import org.chovy.canvas.engine.delivery.ReachDeliveryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = "${canvas.delivery.outbox.topic:CANVAS_DELIVERY}",
        consumerGroup = "${canvas.delivery.outbox.consumer-group:GID_CANVAS_DELIVERY}",
        selectorType = SelectorType.TAG,
        selectorExpression = "*",
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING,
        consumeThreadNumber = 10
)
/**
 * DeliveryOutboxConsumer 封装本模块的核心职责、输入输出结构和协作边界。
 */
public class DeliveryOutboxConsumer implements RocketMQListener<MessageExt> {

    private final DeliveryOutboxService outboxService;
    private final ReachDeliveryService deliveryService;
    private final int maxAttempts;
    private final long retryBaseDelayMs;
    private final long retryMaxDelayMs;

    /**
     * 初始化 DeliveryOutboxConsumer 实例。
     *
     * @param outboxService 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryService 依赖组件，用于完成数据访问或外部能力调用。
     * @param maxAttempts max attempts 参数，用于 DeliveryOutboxConsumer 流程中的校验、计算或对象转换。
     * @param retryBaseDelayMs retry base delay ms 参数，用于 DeliveryOutboxConsumer 流程中的校验、计算或对象转换。
     * @param retryMaxDelayMs retry max delay ms 参数，用于 DeliveryOutboxConsumer 流程中的校验、计算或对象转换。
     */
    public DeliveryOutboxConsumer(DeliveryOutboxService outboxService,
                                  ReachDeliveryService deliveryService,
                                  @Value("${canvas.delivery.outbox.max-attempts:3}") int maxAttempts,
                                  @Value("${canvas.delivery.outbox.retry-base-delay-ms:1000}") long retryBaseDelayMs,
                                  @Value("${canvas.delivery.outbox.retry-max-delay-ms:60000}") long retryMaxDelayMs) {
        this.outboxService = outboxService;
        this.deliveryService = deliveryService;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.retryBaseDelayMs = Math.max(1, retryBaseDelayMs);
        this.retryMaxDelayMs = Math.max(this.retryBaseDelayMs, retryMaxDelayMs);
    }

    @Override
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     */
    public void onMessage(MessageExt message) {
        String workerId = "delivery-mq-" + (message == null ? UUID.randomUUID() : message.getMsgId());
        DispatchResult result = dispatchNext(workerId).block();
        log.info("[DELIVERY_OUTBOX] wakeup consumed workerId={} dispatched={} status={} outboxId={}",
                workerId,
                result != null && result.dispatched(),
                result == null ? null : result.status(),
                result == null ? null : result.outboxId());
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @return 返回流程执行后的业务结果。
     */
    public Mono<DispatchResult> dispatchNext() {
        return dispatchNext("delivery-worker-" + UUID.randomUUID());
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param workerId 业务对象 ID，用于定位具体记录。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<DispatchResult> dispatchNext(String workerId) {
        return Mono.fromCallable(() -> outboxService.claimNext(workerId, LocalDateTime.now()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optional -> optional
                        .map(this::dispatchClaimed)
                        .orElseGet(() -> Mono.just(DispatchResult.empty())));
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param outbox outbox 参数，用于 dispatchClaimed 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private Mono<DispatchResult> dispatchClaimed(DeliveryOutboxDO outbox) {
        return deliveryService.dispatchToProvider(outbox)
                .flatMap(response -> Mono.fromCallable(() -> {
                    String providerMessageId = providerMessageId(response);
                    outboxService.markSent(outbox.getId(), providerMessageId, response);
                    log.info("[DELIVERY_OUTBOX] sent outboxId={} channel={} provider={} attempt={}",
                            outbox.getId(), outbox.getChannel(), outbox.getProvider(), outbox.getAttemptCount());
                    return DispatchResult.sent(outbox.getId(), providerMessageId);
                }).subscribeOn(Schedulers.boundedElastic()))
                .onErrorResume(error -> Mono.fromCallable(() -> handleFailure(outbox, error))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param outbox outbox 参数，用于 handleFailure 流程中的校验、计算或对象转换。
     * @param error error 参数，用于 handleFailure 流程中的校验、计算或对象转换。
     * @return 返回 handleFailure 流程生成的业务结果。
     */
    private DispatchResult handleFailure(DeliveryOutboxDO outbox, Throwable error) {
        String message = error.getMessage() == null ? "delivery provider call failed" : error.getMessage();
        int nextAttempt = outbox.getAttemptCount() + 1;
        if (nextAttempt >= maxAttempts) {
            outboxService.markDead(outbox.getId(), message);
            log.warn("[DELIVERY_OUTBOX] dead outboxId={} channel={} provider={} attempt={} reason={}",
                    outbox.getId(), outbox.getChannel(), outbox.getProvider(), nextAttempt, message);
            return DispatchResult.dead(outbox.getId(), message);
        }

        LocalDateTime nextRetryAt = LocalDateTime.now().plusNanos(backoffMillis(nextAttempt) * 1_000_000L);
        outboxService.markRetry(outbox.getId(), message, nextRetryAt);
        log.warn("[DELIVERY_OUTBOX] retry outboxId={} channel={} provider={} attempt={} nextRetryAt={} reason={}",
                outbox.getId(), outbox.getChannel(), outbox.getProvider(), nextAttempt, nextRetryAt, message);
        return DispatchResult.retry(outbox.getId(), nextRetryAt, message);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param attempt attempt 参数，用于 backoffMillis 流程中的校验、计算或对象转换。
     * @return 返回 backoff millis 计算得到的数量、金额或指标值。
     */
    private long backoffMillis(int attempt) {
        long multiplier = 1L << Math.min(attempt - 1, 20);
        long delay = retryBaseDelayMs * multiplier;
        return Math.min(delay, retryMaxDelayMs);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param MapString map string 参数，用于 providerMessageId 流程中的校验、计算或对象转换。
     * @param response response 参数，用于 providerMessageId 流程中的校验、计算或对象转换。
     * @return 返回 provider message id 生成的文本或业务键。
     */
    private String providerMessageId(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            return null;
        }
        Object messageId = response.getOrDefault(MapFieldKeys.MESSAGE_ID, response.get(MapFieldKeys.ID));
        return messageId == null ? null : messageId.toString();
    }

    /**
     * DispatchResult 封装本模块的核心职责、输入输出结构和协作边界。
     */
    public record DispatchResult(
            boolean dispatched,
            Long outboxId,
            String status,
            String providerMessageId,
            LocalDateTime nextRetryAt,
            String errorMessage
    ) {
        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @return 返回 empty 流程生成的业务结果。
         */
        static DispatchResult empty() {
            return new DispatchResult(false, null, "EMPTY", null, null, null);
        }

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param outboxId 业务对象 ID，用于定位具体记录。
         * @param providerMessageId 业务对象 ID，用于定位具体记录。
         * @return 返回 sent 流程生成的业务结果。
         */
        static DispatchResult sent(Long outboxId, String providerMessageId) {
            return new DispatchResult(true, outboxId, DeliveryOutboxService.STATUS_SENT, providerMessageId, null, null);
        }

        /**
         * 执行核心业务流程，并协调依赖组件完成处理。
         *
         * @param outboxId 业务对象 ID，用于定位具体记录。
         * @param nextRetryAt 时间参数，用于计算窗口、过期或审计时间。
         * @param errorMessage error message 参数，用于 retry 流程中的校验、计算或对象转换。
         * @return 返回流程执行后的业务结果。
         */
        static DispatchResult retry(Long outboxId, LocalDateTime nextRetryAt, String errorMessage) {
            return new DispatchResult(true, outboxId, DeliveryOutboxService.STATUS_RETRY, null, nextRetryAt, errorMessage);
        }

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param outboxId 业务对象 ID，用于定位具体记录。
         * @param errorMessage error message 参数，用于 dead 流程中的校验、计算或对象转换。
         * @return 返回 dead 流程生成的业务结果。
         */
        static DispatchResult dead(Long outboxId, String errorMessage) {
            return new DispatchResult(true, outboxId, DeliveryOutboxService.STATUS_DEAD, null, null, errorMessage);
        }
    }
}
