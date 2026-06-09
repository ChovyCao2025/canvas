package org.chovy.canvas.domain.monitoring;

/**
 * MarketingMonitorWebhookIngestView 承载 domain.monitoring 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param sourceId sourceId 字段。
 * @param sourceKey sourceKey 字段。
 * @param result result 字段。
 */
public record MarketingMonitorWebhookIngestView(Long tenantId,
                                                Long sourceId,
                                                String sourceKey,
                                                MarketingMonitorIngestResult result) {
}
