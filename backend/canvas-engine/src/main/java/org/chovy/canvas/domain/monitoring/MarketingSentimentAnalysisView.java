package org.chovy.canvas.domain.monitoring;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * MarketingSentimentAnalysisView 承载 domain.monitoring 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param itemId itemId 字段。
 * @param sentimentLabel sentimentLabel 字段。
 * @param sentimentScore sentimentScore 字段。
 * @param confidence confidence 字段。
 * @param modelKey modelKey 字段。
 * @param modelVersion modelVersion 字段。
 * @param keywordHits keywordHits 字段。
 * @param createdAt createdAt 字段。
 */
public record MarketingSentimentAnalysisView(Long id,
                                             Long tenantId,
                                             Long itemId,
                                             String sentimentLabel,
                                             BigDecimal sentimentScore,
                                             BigDecimal confidence,
                                             String modelKey,
                                             String modelVersion,
                                             Map<String, Object> keywordHits,
                                             LocalDateTime createdAt) {
}
