package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;
import java.util.Map;

public record CustomerProfile(
        Long id,
        Long tenantId,
        String userId,
        String displayName,
        String phone,
        String email,
        String status,
        Map<String, Object> properties,
        LocalDateTime firstSeenAt,
        LocalDateTime lastSeenAt,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public CustomerProfile withId(Long newId) {
        return new CustomerProfile(newId, tenantId, userId, displayName, phone, email, status, properties,
                firstSeenAt, lastSeenAt, createdBy, createdAt, updatedAt);
    }

    public CustomerProfile withSeenAt(LocalDateTime firstSeen, LocalDateTime lastSeen) {
        return new CustomerProfile(id, tenantId, userId, displayName, phone, email, status, properties,
                firstSeen, lastSeen, createdBy, createdAt, updatedAt);
    }
}
