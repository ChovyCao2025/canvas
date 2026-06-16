package org.chovy.canvas.bi.api;
/**
 * BiDatasetFieldView 视图。
 */
public record BiDatasetFieldView(
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
        boolean visible,
        /**
         * 排序号。
         */
        int sortOrder
) {
}
