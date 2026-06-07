package org.chovy.canvas.domain.conversation;

public record PrivateDomainContactQuery(
        String provider,
        String ownerUserId,
        String keyword,
        int limit) {
}
