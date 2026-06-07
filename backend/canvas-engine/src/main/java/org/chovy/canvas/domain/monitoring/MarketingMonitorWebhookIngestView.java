package org.chovy.canvas.domain.monitoring;

public record MarketingMonitorWebhookIngestView(Long tenantId,
                                                Long sourceId,
                                                String sourceKey,
                                                MarketingMonitorIngestResult result) {
}
