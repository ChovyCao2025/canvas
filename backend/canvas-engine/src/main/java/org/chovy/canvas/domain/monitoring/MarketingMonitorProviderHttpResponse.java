package org.chovy.canvas.domain.monitoring;

import java.util.Map;

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
