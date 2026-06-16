package org.chovy.canvas.bi.api;
/**
 * BiDatasourceHealthView 视图。
 */
public record BiDatasourceHealthView(
        /**
         * sourceKey 对应的业务键。
         */
        String sourceKey,
        /**
         * sourceType 字段值。
         */
        String sourceType,
        /**
         * available 字段值。
         */
        boolean available,
        String message) {
}
