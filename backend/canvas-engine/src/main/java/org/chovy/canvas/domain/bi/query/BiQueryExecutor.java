package org.chovy.canvas.domain.bi.query;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface BiQueryExecutor {

    List<Map<String, Object>> execute(BiCompiledQuery query, BiDatasetSpec dataset);

    default List<Map<String, Object>> execute(BiCompiledQuery query, BiDatasetSpec dataset, String sqlHash) {
        return execute(query, dataset);
    }

    default List<String> explain(BiCompiledQuery query, BiDatasetSpec dataset) {
        return List.of("SQL: " + query.sql());
    }

    default boolean cancel(String sqlHash) {
        return false;
    }
}
