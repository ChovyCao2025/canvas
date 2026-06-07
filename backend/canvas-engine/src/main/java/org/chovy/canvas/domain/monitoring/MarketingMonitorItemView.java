package org.chovy.canvas.domain.monitoring;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
