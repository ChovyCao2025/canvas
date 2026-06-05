package org.chovy.canvas.domain.bi.query;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface BiQueryExecutor {

    List<Map<String, Object>> execute(BiCompiledQuery query, BiDatasetSpec dataset);
}
