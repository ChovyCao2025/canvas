package org.chovy.canvas.bi.api;
/**
 * BiEmbedTicketCleanupResult 结果。
 */
public record BiEmbedTicketCleanupResult(
        /**
         * checked 字段值。
         */
        int checked,
        /**
         * revoked 字段值。
         */
        int revoked,
        int failed) {
}
