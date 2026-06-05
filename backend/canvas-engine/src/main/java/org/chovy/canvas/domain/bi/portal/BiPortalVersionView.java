package org.chovy.canvas.domain.bi.portal;

import java.time.LocalDateTime;

public record BiPortalVersionView(
        Long id,
        String portalKey,
        Integer version,
        String status,
        BiPortalResource resource,
        String publishedBy,
        LocalDateTime createdAt) {
}
