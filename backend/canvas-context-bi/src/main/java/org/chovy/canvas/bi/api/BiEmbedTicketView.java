package org.chovy.canvas.bi.api;

import java.time.Instant;
/**
 * BiEmbedTicketView 视图。
 */
public record BiEmbedTicketView(
        /**
         * ticket 字段值。
         */
        String ticket,
        /**
         * expiresAt 对应的时间。
         */
        Instant expiresAt,
        String embedUrl) {
}
