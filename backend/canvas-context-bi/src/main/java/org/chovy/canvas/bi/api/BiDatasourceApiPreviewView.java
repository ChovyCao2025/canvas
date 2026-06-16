package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;
/**
 * BiDatasourceApiPreviewView 视图。
 */
public record BiDatasourceApiPreviewView(
        /**
         * dataSourceConfigId 对应的标识。
         */
        Long dataSourceConfigId,
        /**
         * sourceKey 对应的业务键。
         */
        String sourceKey,
        /**
         * columns 对应的数据集合。
         */
        List<Map<String, Object>> columns,
        List<Map<String, Object>> rows) {

    public BiDatasourceApiPreviewView {
        columns = columns == null ? List.of() : List.copyOf(columns);
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}
