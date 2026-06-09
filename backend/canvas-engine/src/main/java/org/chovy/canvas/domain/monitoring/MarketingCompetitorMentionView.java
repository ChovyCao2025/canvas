package org.chovy.canvas.domain.monitoring;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * MarketingCompetitorMentionView 承载 domain.monitoring 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param itemId itemId 字段。
 * @param competitorKey competitorKey 字段。
 * @param competitorName competitorName 字段。
 * @param matchedTerms matchedTerms 字段。
 * @param sentimentLabel sentimentLabel 字段。
 * @param sentimentScore sentimentScore 字段。
 * @param createdAt createdAt 字段。
 */
public record MarketingCompetitorMentionView(Long id,
                                             Long tenantId,
                                             Long itemId,
                                             String competitorKey,
                                             String competitorName,
                                             List<String> matchedTerms,
                                             String sentimentLabel,
                                             BigDecimal sentimentScore,
                                             LocalDateTime createdAt) {
}
