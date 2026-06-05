package org.chovy.canvas.domain.bi.subscription;

public record BiDeliverySchedulerResult(
        int subscriptionsChecked,
        int subscriptionsTriggered,
        int alertsChecked,
        int alertsTriggered,
        int skipped,
        int failed
) {
}
