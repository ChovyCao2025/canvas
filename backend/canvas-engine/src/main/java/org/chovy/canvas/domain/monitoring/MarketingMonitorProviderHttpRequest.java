package org.chovy.canvas.domain.monitoring;

import java.net.URI;
import java.util.Map;

/**
 * MarketingMonitorProviderHttpRequest 承载 domain.monitoring 场景中的不可变数据快照。
 * @param method method 字段。
 * @param uri uri 字段。
 * @param headers headers 字段。
 * @param body body 字段。
 */
public record MarketingMonitorProviderHttpRequest(
        String method,
        URI uri,
        Map<String, String> headers,
        String body
) {
    public MarketingMonitorProviderHttpRequest {
        method = method == null || method.isBlank() ? "GET" : method.trim().toUpperCase();
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        body = body == null ? "" : body;
    }
}
