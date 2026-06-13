package org.chovy.canvas.bi.api;

public record BiQuickEngineTenantPoolPolicyCommand(
        String poolKey,
        Integer maxConcurrentQueries,
        Integer queueLimit,
        Integer queueTimeoutSeconds,
        Integer poolWeight) {
}
