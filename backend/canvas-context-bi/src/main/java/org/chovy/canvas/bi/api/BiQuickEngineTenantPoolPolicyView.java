package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;

public record BiQuickEngineTenantPoolPolicyView(
        String poolKey,
        Integer maxConcurrentQueries,
        Integer queueLimit,
        Integer queueTimeoutSeconds,
        Integer poolWeight,
        String updatedBy,
        LocalDateTime updatedAt) {
}
