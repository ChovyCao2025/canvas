package org.chovy.canvas.domain.bi.datasource;

import java.util.Map;

/**
 * BiDatasourceApiPreviewRequest 承载 domain.bi.datasource 场景中的不可变数据快照。
 * @param variables variables 字段。
 * @param limit limit 字段。
 */
public record BiDatasourceApiPreviewRequest(
        Map<String, String> variables,
        Integer limit
) {

    public BiDatasourceApiPreviewRequest {
        variables = variables == null ? Map.of() : Map.copyOf(variables);
    }
}
