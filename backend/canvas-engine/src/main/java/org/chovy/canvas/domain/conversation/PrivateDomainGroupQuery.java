package org.chovy.canvas.domain.conversation;

public record PrivateDomainGroupQuery(
        String provider,
        String ownerUserId,
        int limit) {
}
