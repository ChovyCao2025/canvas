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
/**
 * DeliveryReconciliationJob 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
 */
public class DeliveryReconciliationJob {

    private final DeliveryOutboxService outboxService;

    @Value("${canvas.delivery.outbox.reconciliation.enabled:false}")
    private boolean enabled;

    @Value("${canvas.delivery.outbox.stale-seconds:300}")
    private long staleSeconds;

    @Value("${canvas.delivery.outbox.reconciliation.limit:100}")
    private int limit;

    @Scheduled(fixedDelayString = "${canvas.delivery.outbox.reconciliation.fixed-delay-ms:60000}")
    /**
     * 根据方法职责完成对应的业务处理流程。
     */
    public void reconcileScheduled() {
        if (!enabled) {
            return;
        }
        int requeued = reconcile();
        if (requeued > 0) {
            log.info("[DELIVERY_OUTBOX] reconciliation requeued stale deliveries count={}", requeued);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 reconcile 计算得到的数量、金额或指标值。
     */
    public int reconcile() {
        return outboxService.requeueStalePending(LocalDateTime.now().minusSeconds(staleSeconds), limit);
    }
}
