package org.chovy.canvas.domain.bi.dataset;

public record BiQuickEngineQueueAdmissionCommand(
        String poolKey,
        String sqlHash,
        String datasetKey,
        String requestedBy,
        Integer queueTimeoutSeconds) {
}
