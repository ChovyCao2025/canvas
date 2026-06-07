package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record MarketingMonitorPollItem(
        String externalItemId,
        String sourceUrl,
        String authorKey,
        String brandKey,
        String text,
        String language,
        LocalDateTime publishedAt,
        Map<String, Object> rawPayload) {

    public MarketingMonitorPollItem {
        rawPayload = rawPayload == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(rawPayload));
    }
}
