package org.chovy.canvas.bi.api;
/**
 * BiDatasetFieldCommand 命令。
 */
public record BiDatasetFieldCommand(
        /**
         * fieldKey 对应的业务键。
         */
        String fieldKey,
        /**
         * displayName 字段值。
         */
        String displayName,
        /**
         * columnExpression 字段值。
         */
        String columnExpression,
        /**
         * roleKey 对应的业务键。
         */
        String roleKey,
        /**
         * dataType 字段值。
         */
        String dataType,
        /**
         * defaultAggregation 字段值。
         */
        String defaultAggregation,
        /**
         * visible 字段值。
         */
        Boolean visible,
        /**
         * 排序号。
         */
        Integer sortOrder
) {
}
