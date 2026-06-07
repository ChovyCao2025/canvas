package org.chovy.canvas.domain.bi.dataset;

public record BiQuickEngineTenantPoolPolicyCommand(
        String poolKey,
        Integer maxConcurrentQueries,
        Integer queueLimit,
        Integer queueTimeoutSeconds,
        Integer poolWeight) {
}
