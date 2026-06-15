package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;

public record BiDatasourceApiPreviewView(
        Long dataSourceConfigId,
        String sourceKey,
        List<Map<String, Object>> columns,
        List<Map<String, Object>> rows) {

    public BiDatasourceApiPreviewView {
        columns = columns == null ? List.of() : List.copyOf(columns);
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}
