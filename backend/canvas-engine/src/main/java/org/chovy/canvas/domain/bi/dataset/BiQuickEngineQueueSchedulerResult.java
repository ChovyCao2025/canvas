package org.chovy.canvas.domain.bi.dataset;

public record BiQuickEngineQueueSchedulerResult(
        int expired,
        int recovered,
        int claimed,
        int skipped) {
}
