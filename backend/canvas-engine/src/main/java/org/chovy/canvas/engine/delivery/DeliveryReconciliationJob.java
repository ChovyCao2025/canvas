package org.chovy.canvas.engine.delivery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryReconciliationJob {

    private final DeliveryOutboxService outboxService;

    @Value("${canvas.delivery.outbox.reconciliation.enabled:false}")
    private boolean enabled;

    @Value("${canvas.delivery.outbox.stale-seconds:300}")
    private long staleSeconds;

    @Value("${canvas.delivery.outbox.reconciliation.limit:100}")
    private int limit;

    @Scheduled(fixedDelayString = "${canvas.delivery.outbox.reconciliation.fixed-delay-ms:60000}")
    public void reconcileScheduled() {
        if (!enabled) {
            return;
        }
        int requeued = reconcile();
        if (requeued > 0) {
            log.info("[DELIVERY_OUTBOX] reconciliation requeued stale deliveries count={}", requeued);
        }
    }

    public int reconcile() {
        return outboxService.requeueStalePending(LocalDateTime.now().minusSeconds(staleSeconds), limit);
    }
}
