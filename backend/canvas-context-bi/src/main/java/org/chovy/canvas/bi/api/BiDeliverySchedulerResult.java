package org.chovy.canvas.bi.api;

public record BiDeliverySchedulerResult(
        int subscriptionsChecked,
        int subscriptionsTriggered,
        int alertsChecked,
        int alertsTriggered,
        int skipped,
        int failed) {
}
