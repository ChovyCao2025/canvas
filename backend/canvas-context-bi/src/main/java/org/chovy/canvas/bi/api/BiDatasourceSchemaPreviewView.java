package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;

public record BiDatasourceSchemaPreviewView(
        Long dataSourceConfigId,
        String sourceKey,
        List<Map<String, Object>> tables) {

    public BiDatasourceSchemaPreviewView {
        tables = tables == null ? List.of() : List.copyOf(tables);
    }
}
