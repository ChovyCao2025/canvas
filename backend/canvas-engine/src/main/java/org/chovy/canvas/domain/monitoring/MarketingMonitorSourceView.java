package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.Map;

public record MarketingMonitorSourceView(Long id,
                                         Long tenantId,
                                         String sourceKey,
                                         String sourceType,
                                         String displayName,
                                         boolean enabled,
                                         Map<String, Object> metadata,
                                         String createdBy,
                                         LocalDateTime createdAt,
                                         LocalDateTime updatedAt) {
}
