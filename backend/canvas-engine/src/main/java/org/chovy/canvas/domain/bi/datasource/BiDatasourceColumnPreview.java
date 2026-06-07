package org.chovy.canvas.domain.bi.datasource;

public record BiDatasourceColumnPreview(
        String name,
        String typeName,
        int dataType,
        boolean nullable,
        int ordinalPosition) {
}
