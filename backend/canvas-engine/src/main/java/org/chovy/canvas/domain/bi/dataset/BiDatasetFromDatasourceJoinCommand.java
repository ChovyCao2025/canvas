package org.chovy.canvas.domain.bi.dataset;

import java.util.List;

public record BiDatasetFromDatasourceJoinCommand(
        String joinType,
        String leftAlias,
        String leftColumn,
        String rightAlias,
        String rightColumn,
        List<BiDatasetFromDatasourceJoinConditionCommand> conditions
) {
    public BiDatasetFromDatasourceJoinCommand(
            String joinType,
            String leftAlias,
            String leftColumn,
            String rightAlias,
            String rightColumn) {
        this(joinType, leftAlias, leftColumn, rightAlias, rightColumn, null);
    }

    public BiDatasetFromDatasourceJoinCommand {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }
}
