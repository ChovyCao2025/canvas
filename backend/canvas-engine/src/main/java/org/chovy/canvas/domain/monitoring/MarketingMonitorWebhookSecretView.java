package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;

public record MarketingMonitorWebhookSecretView(Long sourceId,
                                                Long tenantId,
                                                String sourceKey,
                                                String secretPrefix,
                                                String signingSecret,
                                                String endpointPath,
                                                Integer toleranceSeconds,
                                                String rotatedBy,
                                                LocalDateTime rotatedAt) {
}
