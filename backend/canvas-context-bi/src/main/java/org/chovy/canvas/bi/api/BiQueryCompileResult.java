package org.chovy.canvas.bi.api;

import java.util.List;
/**
 * BiQueryCompileResult 结果。
 */
public record BiQueryCompileResult(
        /**
         * sql 字段值。
         */
        String sql,
        List<Object> parameters) {

    public BiQueryCompileResult {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
    }
}
