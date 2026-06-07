package org.chovy.canvas.domain.monitoring;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
