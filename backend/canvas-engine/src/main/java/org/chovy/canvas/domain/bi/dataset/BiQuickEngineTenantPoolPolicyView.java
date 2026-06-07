package org.chovy.canvas.domain.bi.dataset;

import java.time.LocalDateTime;

public record BiQuickEngineTenantPoolPolicyView(
        String poolKey,
        int maxConcurrentQueries,
        int queueLimit,
        int queueTimeoutSeconds,
        int poolWeight,
        String updatedBy,
        LocalDateTime updatedAt) {
}
