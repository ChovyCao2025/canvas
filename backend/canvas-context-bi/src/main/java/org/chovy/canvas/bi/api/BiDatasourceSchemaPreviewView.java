package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;
/**
 * BiDatasourceSchemaPreviewView 视图。
 */
public record BiDatasourceSchemaPreviewView(
        /**
         * dataSourceConfigId 对应的标识。
         */
        Long dataSourceConfigId,
        /**
         * sourceKey 对应的业务键。
         */
        String sourceKey,
        List<Map<String, Object>> tables) {

    public BiDatasourceSchemaPreviewView {
        tables = tables == null ? List.of() : List.copyOf(tables);
    }
}
