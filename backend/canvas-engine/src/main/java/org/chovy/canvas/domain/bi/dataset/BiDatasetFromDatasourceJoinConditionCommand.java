package org.chovy.canvas.domain.bi.dataset;

/**
 * BiDatasetFromDatasourceJoinConditionCommand 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param leftColumn leftColumn 字段。
 * @param rightColumn rightColumn 字段。
 * @param operator operator 字段。
 * @param connector connector 字段。
 * @param groupStart groupStart 字段。
 * @param groupEnd groupEnd 字段。
 */
public record BiDatasetFromDatasourceJoinConditionCommand(
        String leftColumn,
        String rightColumn,
        String operator,
        String connector,
        boolean groupStart,
        boolean groupEnd
) {
    /**
     * 创建 BiDatasetFromDatasourceJoinConditionCommand 实例并注入 domain.bi.dataset 场景依赖。
     * @param leftColumn left column 参数，用于 BiDatasetFromDatasourceJoinConditionCommand 流程中的校验、计算或对象转换。
     * @param rightColumn right column 参数，用于 BiDatasetFromDatasourceJoinConditionCommand 流程中的校验、计算或对象转换。
     */
    public BiDatasetFromDatasourceJoinConditionCommand(String leftColumn, String rightColumn) {
        this(leftColumn, rightColumn, null, null, false, false);
    }

    /**
     * 创建 BiDatasetFromDatasourceJoinConditionCommand 实例并注入 domain.bi.dataset 场景依赖。
     * @param leftColumn left column 参数，用于 BiDatasetFromDatasourceJoinConditionCommand 流程中的校验、计算或对象转换。
     * @param rightColumn right column 参数，用于 BiDatasetFromDatasourceJoinConditionCommand 流程中的校验、计算或对象转换。
     * @param operator 操作人标识，用于审计和权限判断。
     */
    public BiDatasetFromDatasourceJoinConditionCommand(String leftColumn, String rightColumn, String operator) {
        this(leftColumn, rightColumn, operator, null, false, false);
    }

    /**
     * 创建 BiDatasetFromDatasourceJoinConditionCommand 实例并注入 domain.bi.dataset 场景依赖。
     * @param leftColumn left column 参数，用于 BiDatasetFromDatasourceJoinConditionCommand 流程中的校验、计算或对象转换。
     * @param rightColumn right column 参数，用于 BiDatasetFromDatasourceJoinConditionCommand 流程中的校验、计算或对象转换。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param connector connector 参数，用于 BiDatasetFromDatasourceJoinConditionCommand 流程中的校验、计算或对象转换。
     */
    public BiDatasetFromDatasourceJoinConditionCommand(String leftColumn,
                                                       String rightColumn,
                                                       String operator,
                                                       String connector) {
        this(leftColumn, rightColumn, operator, connector, false, false);
    }
}
