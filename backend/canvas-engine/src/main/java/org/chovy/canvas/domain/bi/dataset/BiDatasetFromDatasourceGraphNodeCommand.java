package org.chovy.canvas.domain.bi.dataset;

public record BiDatasetFromDatasourceGraphNodeCommand(
        String tableName,
        String alias,
        Integer x,
        Integer y
) {
}
