package org.chovy.canvas.domain.monitoring;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

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
