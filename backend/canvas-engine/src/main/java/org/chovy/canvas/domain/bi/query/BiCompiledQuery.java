package org.chovy.canvas.domain.bi.query;

import java.util.List;

/**
 * BiCompiledQuery 承载 domain.bi.query 场景中的不可变数据快照。
 * @param sql sql 字段。
 * @param parameters parameters 字段。
 */
public record BiCompiledQuery(
        String sql,
        List<Object> parameters
) {
    public BiCompiledQuery {
        parameters = List.copyOf(parameters);
    }
}
