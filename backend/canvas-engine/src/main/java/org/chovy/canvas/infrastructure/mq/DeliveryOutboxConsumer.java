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
public class DeliveryOutboxConsumer implements RocketMQListener<MessageExt> {

    private final DeliveryOutboxService outboxService;
    private final ReachDeliveryService deliveryService;
    private final int maxAttempts;
    private final long retryBaseDelayMs;
    private final long retryMaxDelayMs;

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
    public void onMessage(MessageExt message) {
        String workerId = "delivery-mq-" + (message == null ? UUID.randomUUID() : message.getMsgId());
        DispatchResult result = dispatchNext(workerId).block();
        log.info("[DELIVERY_OUTBOX] wakeup consumed workerId={} dispatched={} status={} outboxId={}",
                workerId,
                result != null && result.dispatched(),
                result == null ? null : result.status(),
                result == null ? null : result.outboxId());
    }

    public Mono<DispatchResult> dispatchNext() {
        return dispatchNext("delivery-worker-" + UUID.randomUUID());
    }

    public Mono<DispatchResult> dispatchNext(String workerId) {
        return Mono.fromCallable(() -> outboxService.claimNext(workerId, LocalDateTime.now()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optional -> optional
                        .map(this::dispatchClaimed)
                        .orElseGet(() -> Mono.just(DispatchResult.empty())));
    }

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

    private long backoffMillis(int attempt) {
        long multiplier = 1L << Math.min(attempt - 1, 20);
        long delay = retryBaseDelayMs * multiplier;
        return Math.min(delay, retryMaxDelayMs);
    }

    private String providerMessageId(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            return null;
        }
        Object messageId = response.getOrDefault(MapFieldKeys.MESSAGE_ID, response.get(MapFieldKeys.ID));
        return messageId == null ? null : messageId.toString();
    }

    public record DispatchResult(
            boolean dispatched,
            Long outboxId,
            String status,
            String providerMessageId,
            LocalDateTime nextRetryAt,
            String errorMessage
    ) {
        static DispatchResult empty() {
            return new DispatchResult(false, null, "EMPTY", null, null, null);
        }

        static DispatchResult sent(Long outboxId, String providerMessageId) {
            return new DispatchResult(true, outboxId, DeliveryOutboxService.STATUS_SENT, providerMessageId, null, null);
        }

        static DispatchResult retry(Long outboxId, LocalDateTime nextRetryAt, String errorMessage) {
            return new DispatchResult(true, outboxId, DeliveryOutboxService.STATUS_RETRY, null, nextRetryAt, errorMessage);
        }

        static DispatchResult dead(Long outboxId, String errorMessage) {
            return new DispatchResult(true, outboxId, DeliveryOutboxService.STATUS_DEAD, null, null, errorMessage);
        }
    }
}
