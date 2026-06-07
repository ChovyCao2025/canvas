package org.chovy.canvas.domain.bi.dataset;

public record BiDatasetFromDatasourceJoinConditionCommand(
        String leftColumn,
        String rightColumn,
        String operator
) {
    public BiDatasetFromDatasourceJoinConditionCommand(String leftColumn, String rightColumn) {
        this(leftColumn, rightColumn, null);
    }
}
