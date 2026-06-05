package org.chovy.canvas.domain.bi.query;

import java.util.List;

public record BiCompiledQuery(
        String sql,
        List<Object> parameters
) {
    public BiCompiledQuery {
        parameters = List.copyOf(parameters);
    }
}
