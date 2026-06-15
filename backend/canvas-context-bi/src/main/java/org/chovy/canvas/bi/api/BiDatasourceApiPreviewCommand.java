package org.chovy.canvas.bi.api;

import java.util.Map;

public record BiDatasourceApiPreviewCommand(
        Map<String, Object> parameters,
        String path,
        String method,
        Map<String, String> headers,
        Map<String, Object> body,
        int limit) {

    public BiDatasourceApiPreviewCommand {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        body = body == null ? Map.of() : Map.copyOf(body);
    }
}
