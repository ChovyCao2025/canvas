package org.chovy.canvas.domain.bi.datasource;

import java.util.List;

public record BiDatasourceTablePreview(
        String name,
        String tableType,
        List<BiDatasourceColumnPreview> columns) {

    public BiDatasourceTablePreview {
        columns = columns == null ? List.of() : List.copyOf(columns);
    }
}
