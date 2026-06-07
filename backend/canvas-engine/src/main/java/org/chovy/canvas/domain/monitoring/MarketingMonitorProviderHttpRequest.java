package org.chovy.canvas.domain.monitoring;

import java.net.URI;
import java.util.Map;

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
