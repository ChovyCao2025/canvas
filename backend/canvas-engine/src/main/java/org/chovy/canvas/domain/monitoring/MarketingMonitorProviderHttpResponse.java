package org.chovy.canvas.domain.monitoring;

import java.util.Map;

/**
 * MarketingMonitorProviderHttpResponse 承载 domain.monitoring 场景中的不可变数据快照。
 * @param statusCode statusCode 字段。
 * @param body body 字段。
 * @param headers headers 字段。
 */
public record MarketingMonitorProviderHttpResponse(
        int statusCode,
        String body,
        Map<String, String> headers
) {
    public MarketingMonitorProviderHttpResponse {
        body = body == null ? "" : body;
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }
}
