package org.chovy.canvas.bi.api;

import java.util.List;

public record BiQueryCompileResult(
        String sql,
        List<Object> parameters) {

    public BiQueryCompileResult {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
    }
}
