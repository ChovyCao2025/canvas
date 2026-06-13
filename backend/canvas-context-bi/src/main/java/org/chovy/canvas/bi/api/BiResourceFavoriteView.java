package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;

public record BiResourceFavoriteView(
        Long tenantId,
        String actor,
        String resourceType,
        String resourceKey,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
