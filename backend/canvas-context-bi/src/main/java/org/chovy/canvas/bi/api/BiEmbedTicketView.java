package org.chovy.canvas.bi.api;

import java.time.Instant;

public record BiEmbedTicketView(
        String ticket,
        Instant expiresAt,
        String embedUrl) {
}
