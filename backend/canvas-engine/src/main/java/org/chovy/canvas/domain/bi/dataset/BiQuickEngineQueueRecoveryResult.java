package org.chovy.canvas.domain.bi.dataset;

public record BiQuickEngineQueueRecoveryResult(
        int expired,
        int recovered) {
}
