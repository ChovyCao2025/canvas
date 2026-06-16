package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;
/**
 * BiDatasetCommand 命令。
 */
public record BiDatasetCommand(
        /**
         * 工作空间标识。
         */
        Long workspaceId,
        /**
         * 数据集键。
         */
        String datasetKey,
        /**
         * 展示名称。
         */
        String name,
        /**
         * datasetType 字段值。
         */
        String datasetType,
        /**
         * sourceRefId 对应的标识。
         */
        Long sourceRefId,
        /**
         * tableExpression 字段值。
         */
        String tableExpression,
        /**
         * tenantColumn 字段值。
         */
        String tenantColumn,
        /**
         * model 字段值。
         */
        Map<String, Object> model,
        /**
         * 字段列表。
         */
        List<BiDatasetFieldCommand> fields,
        /**
         * 指标列表。
         */
        List<BiMetricCommand> metrics,
        /**
         * 状态值。
         */
        String status
) {
    public BiDatasetCommand {
        model = model == null ? Map.of() : Map.copyOf(model);
        fields = fields == null ? List.of() : List.copyOf(fields);
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
    }
}
