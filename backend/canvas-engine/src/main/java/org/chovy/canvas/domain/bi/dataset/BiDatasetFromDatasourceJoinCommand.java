package org.chovy.canvas.domain.bi.dataset;

import java.util.List;

/**
 * BiDatasetFromDatasourceJoinCommand 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param joinType joinType 字段。
 * @param leftAlias leftAlias 字段。
 * @param leftColumn leftColumn 字段。
 * @param rightAlias rightAlias 字段。
 * @param rightColumn rightColumn 字段。
 * @param conditions conditions 字段。
 */
public record BiDatasetFromDatasourceJoinCommand(
        String joinType,
        String leftAlias,
        String leftColumn,
        String rightAlias,
        String rightColumn,
        List<BiDatasetFromDatasourceJoinConditionCommand> conditions
) {
    /**
     * 创建 BiDatasetFromDatasourceJoinCommand 实例并注入 domain.bi.dataset 场景依赖。
     * @param joinType 类型标识，用于选择对应处理分支。
     * @param leftAlias left alias 参数，用于 BiDatasetFromDatasourceJoinCommand 流程中的校验、计算或对象转换。
     * @param leftColumn left column 参数，用于 BiDatasetFromDatasourceJoinCommand 流程中的校验、计算或对象转换。
     * @param rightAlias right alias 参数，用于 BiDatasetFromDatasourceJoinCommand 流程中的校验、计算或对象转换。
     * @param rightColumn right column 参数，用于 BiDatasetFromDatasourceJoinCommand 流程中的校验、计算或对象转换。
     */
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
