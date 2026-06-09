package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MarketingMonitorPollItem 承载 domain.monitoring 场景中的不可变数据快照。
 * @param externalItemId externalItemId 字段。
 * @param sourceUrl sourceUrl 字段。
 * @param authorKey authorKey 字段。
 * @param brandKey brandKey 字段。
 * @param text text 字段。
 * @param language language 字段。
 * @param publishedAt publishedAt 字段。
 * @param rawPayload rawPayload 字段。
 */
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
