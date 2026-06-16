package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
/**
 * BiDatasourceHealthSnapshotView 视图。
 */
public record BiDatasourceHealthSnapshotView(
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
        /**
         * message 字段值。
         */
        String message,
        LocalDateTime checkedAt) {
}
