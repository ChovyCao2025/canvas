package org.chovy.canvas.domain.bi.bigscreen;

import java.time.LocalDateTime;

public record BiBigScreenVersionView(
        Long id,
        String screenKey,
        Integer version,
        String status,
        BiBigScreenResource resource,
        String publishedBy,
        LocalDateTime createdAt) {
}
