package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record MarketingMonitorAlertChannelView(Long id,
                                               Long tenantId,
                                               String channelKey,
                                               String channelType,
                                               String displayName,
                                               String endpointUrl,
                                               boolean enabled,
                                               String minSeverity,
                                               List<String> alertTypes,
                                               String signingMode,
                                               String secretPrefix,
                                               Map<String, Object> metadata,
                                               int maxAttempts,
                                               String createdBy,
                                               LocalDateTime createdAt,
                                               LocalDateTime updatedAt) {
}
