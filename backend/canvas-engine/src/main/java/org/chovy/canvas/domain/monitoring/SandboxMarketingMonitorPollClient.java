package org.chovy.canvas.domain.monitoring;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SandboxMarketingMonitorPollClient implements MarketingMonitorPollClient {

    @Override
    public boolean supports(String sourceType) {
        return sourceType != null && ("SANDBOX".equalsIgnoreCase(sourceType) || "MOCK".equalsIgnoreCase(sourceType));
    }

    @Override
    public MarketingMonitorPollResponse fetch(MarketingMonitorPollRequest request) {
        List<MarketingMonitorPollItem> items = new ArrayList<>();
        Object rawItems = request == null ? null : request.sourceMetadata().get("pollItems");
        if (rawItems instanceof List<?> list) {
            for (Object value : list) {
                if (value instanceof Map<?, ?> map) {
                    items.add(toItem(map));
                }
            }
        }
        return new MarketingMonitorPollResponse(
                items.stream().limit(request == null ? 100 : request.maxItems()).toList(),
                cursor(request, items.size()),
                Map.of("client", "sandbox"));
    }

    private MarketingMonitorPollItem toItem(Map<?, ?> map) {
        return new MarketingMonitorPollItem(
                string(map.get("externalItemId")),
                string(map.get("sourceUrl")),
                string(map.get("authorKey")),
                string(map.get("brandKey")),
                string(map.get("text")),
                string(map.get("language")),
                time(map.get("publishedAt")),
                Map.of("sandbox", true, "raw", map));
    }

    private String cursor(MarketingMonitorPollRequest request, int size) {
        String base = request == null || request.cursor() == null || request.cursor().isBlank()
                ? "sandbox"
                : request.cursor();
        return base + ":" + size;
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private LocalDateTime time(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof String text && !text.isBlank()) {
            return LocalDateTime.parse(text);
        }
        return null;
    }
}
