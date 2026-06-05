package org.chovy.canvas.domain.bi.embed;

import java.time.Instant;

public record BiEmbedTicket(
        String ticket,
        Instant expiresAt,
        String embedUrl
) {
}
