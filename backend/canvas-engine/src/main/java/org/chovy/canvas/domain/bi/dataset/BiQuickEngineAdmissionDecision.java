package org.chovy.canvas.domain.bi.dataset;

public record BiQuickEngineAdmissionDecision(
        boolean allowed,
        String status,
        String message,
        BiQuickEngineTenantPoolPolicyView tenantPoolPolicy,
        BiQuickEngineConcurrencyQueueView concurrencyQueue) {

    public boolean queued() {
        return normalize(status).contains("QUEUE") || normalize(message).contains("QUEUED");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
