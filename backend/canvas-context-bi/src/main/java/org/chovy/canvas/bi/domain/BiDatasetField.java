package org.chovy.canvas.bi.domain;
/**
 * BiDatasetField 不可变数据载体。
 */
public record BiDatasetField(
        /**
         * fieldKey 对应的业务键。
         */
        BiResourceKey fieldKey,
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
    public BiDatasetField {
        if (fieldKey == null) {
            throw new IllegalArgumentException("fieldKey is required");
        }
        displayName = displayName == null || displayName.isBlank() ? fieldKey.value() : displayName.trim();
        columnExpression = columnExpression == null || columnExpression.isBlank()
                ? fieldKey.value()
                : columnExpression.trim();
        roleKey = upperOrDefault(roleKey, "DIMENSION");
        dataType = upperOrDefault(dataType, "STRING");
        defaultAggregation = upperOrDefault(defaultAggregation, "NONE");
    }
    /**
     * 执行 upper Or Default 相关处理。
     */
    private static String upperOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
