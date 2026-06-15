package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.Map;

public record BiResourceVersionView(
        String resourceType,
        String resourceKey,
        Integer version,
        String status,
        Map<String, Object> snapshot,
        String createdBy,
        LocalDateTime createdAt) {
}
