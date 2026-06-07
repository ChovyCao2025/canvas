package org.chovy.canvas.domain.bi.datasource;

import java.util.Map;

public record BiDatasourceApiPreviewRequest(
        Map<String, String> variables,
        Integer limit
) {

    public BiDatasourceApiPreviewRequest {
        variables = variables == null ? Map.of() : Map.copyOf(variables);
    }
}
