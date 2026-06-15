package org.chovy.canvas.bi.api;

public record BiEmbedTicketCleanupResult(
        int checked,
        int revoked,
        int failed) {
}
