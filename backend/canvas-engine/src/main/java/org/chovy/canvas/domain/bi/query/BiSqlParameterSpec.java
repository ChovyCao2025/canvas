package org.chovy.canvas.domain.bi.query;

import java.util.List;

/**
 * BiSqlParameterSpec 承载 domain.bi.query 场景中的不可变数据快照。
 * @param key key 字段。
 * @param type type 字段。
 * @param required required 字段。
 * @param defaultValue defaultValue 字段。
 * @param allowedValues allowedValues 字段。
 */
public record BiSqlParameterSpec(
        String key,
        String type,
        boolean required,
        String defaultValue,
        List<String> allowedValues
) {
    public BiSqlParameterSpec {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("SQL parameter key is required");
        }
        key = key.trim();
        type = type == null || type.isBlank() ? "STRING" : type.trim();
        defaultValue = defaultValue == null || defaultValue.isBlank() ? null : defaultValue.trim();
        allowedValues = allowedValues == null ? List.of() : List.copyOf(allowedValues);
    }
}
