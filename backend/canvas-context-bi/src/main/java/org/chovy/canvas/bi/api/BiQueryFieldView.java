package org.chovy.canvas.bi.api;
/**
 * BiQueryFieldView 视图。
 */
public record BiQueryFieldView(
        /**
         * fieldKey 对应的业务键。
         */
        String fieldKey,
        /**
         * role 字段值。
         */
        String role,
        String dataType) {
}
