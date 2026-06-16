package org.chovy.canvas.bi.domain;

import java.util.Locale;
import java.util.Objects;
/**
 * BiResourceKey 不可变数据载体。
 */
public record BiResourceKey(String value) {

    public BiResourceKey {
        value = normalize(value, "resourceKey");
    }
    /**
     * 执行 of 相关处理。
     */
    public static BiResourceKey of(String value, String field) {
        return new BiResourceKey(normalize(value, field));
    }
    /**
     * 规范化输入值。
     */
    private static String normalize(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String normalized = value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return normalized;
    }
    /**
     * 转换为目标数据结构。
     */
    @Override
    public String toString() {
        return Objects.toString(value);
    }
}
