package org.chovy.canvas.domain.bi.embed;

public record BiEmbedTokenCleanupResult(
        int checked,
        int revoked,
        int failed
) {
}
