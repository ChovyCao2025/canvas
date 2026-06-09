package org.chovy.canvas.domain.monitoring;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * MarketingMonitorItemView 承载 domain.monitoring 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param sourceId sourceId 字段。
 * @param externalItemId externalItemId 字段。
 * @param sourceType sourceType 字段。
 * @param sourceUrl sourceUrl 字段。
 * @param authorKey authorKey 字段。
 * @param brandKey brandKey 字段。
 * @param text text 字段。
 * @param language language 字段。
 * @param publishedAt publishedAt 字段。
 * @param ingestedAt ingestedAt 字段。
 * @param rawPayload rawPayload 字段。
 * @param sentimentLabel sentimentLabel 字段。
 * @param sentimentScore sentimentScore 字段。
 * @param confidence confidence 字段。
 * @param competitorKeys competitorKeys 字段。
 */
public record MarketingMonitorItemView(Long id,
                                       Long tenantId,
                                       Long sourceId,
                                       String externalItemId,
                                       String sourceType,
                                       String sourceUrl,
                                       String authorKey,
                                       String brandKey,
                                       String text,
                                       String language,
                                       LocalDateTime publishedAt,
                                       LocalDateTime ingestedAt,
                                       Map<String, Object> rawPayload,
                                       String sentimentLabel,
                                       BigDecimal sentimentScore,
                                       BigDecimal confidence,
                                       List<String> competitorKeys) {
}
