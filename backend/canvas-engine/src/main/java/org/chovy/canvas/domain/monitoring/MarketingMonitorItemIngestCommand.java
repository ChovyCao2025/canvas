package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record MarketingMonitorItemIngestCommand(Long sourceId,
                                                String externalItemId,
                                                String sourceUrl,
                                                String authorKey,
                                                String brandKey,
                                                String text,
                                                String language,
                                                LocalDateTime publishedAt,
                                                Map<String, List<String>> competitors,
                                                Map<String, Object> rawPayload) {
}
