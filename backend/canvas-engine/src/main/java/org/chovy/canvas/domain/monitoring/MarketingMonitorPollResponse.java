package org.chovy.canvas.domain.monitoring;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MarketingMonitorPollResponse 承载 domain.monitoring 场景中的不可变数据快照。
 * @param items items 字段。
 * @param nextCursor nextCursor 字段。
 * @param metadata metadata 字段。
 */
public record MarketingMonitorPollResponse(
        List<MarketingMonitorPollItem> items,
        String nextCursor,
        Map<String, Object> metadata) {

    public MarketingMonitorPollResponse {
        items = items == null ? List.of() : List.copyOf(items);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}
