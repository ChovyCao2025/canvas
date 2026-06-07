package org.chovy.canvas.domain.monitoring;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record MarketingMonitorPollResponse(
        List<MarketingMonitorPollItem> items,
        String nextCursor,
        Map<String, Object> metadata) {

    public MarketingMonitorPollResponse {
        items = items == null ? List.of() : List.copyOf(items);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}
