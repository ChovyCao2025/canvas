package org.chovy.canvas.domain.bi.datasource;

import java.time.LocalDateTime;
import java.util.List;

public record BiDatasourceSchemaPreview(
        Long id,
        String sourceKey,
        String name,
        String connectorType,
        List<BiDatasourceTablePreview> tables,
        LocalDateTime checkedAt) {

    public BiDatasourceSchemaPreview {
        tables = tables == null ? List.of() : List.copyOf(tables);
    }
}
