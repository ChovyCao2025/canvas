package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;

/**
 * MarketingMonitorWebhookSecretView 承载 domain.monitoring 场景中的不可变数据快照。
 * @param sourceId sourceId 字段。
 * @param tenantId tenantId 字段。
 * @param sourceKey sourceKey 字段。
 * @param secretPrefix secretPrefix 字段。
 * @param signingSecret signingSecret 字段。
 * @param endpointPath endpointPath 字段。
 * @param toleranceSeconds toleranceSeconds 字段。
 * @param rotatedBy rotatedBy 字段。
 * @param rotatedAt rotatedAt 字段。
 */
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
