package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;

public record CdpUserTag(
        Long id,
        Long tenantId,
        String userId,
        String tagCode,
        String tagValue,
        String valueType,
        String sourceType,
        String sourceRefId,
        String status,
        LocalDateTime effectiveAt,
        LocalDateTime expiresAt,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public CdpUserTag withId(Long newId) {
        return new CdpUserTag(newId, tenantId, userId, tagCode, tagValue, valueType, sourceType, sourceRefId, status,
                effectiveAt, expiresAt, createdBy, createdAt, updatedAt);
    }

    public CdpUserTag withStatus(String newStatus) {
        return new CdpUserTag(id, tenantId, userId, tagCode, tagValue, valueType, sourceType, sourceRefId, newStatus,
                effectiveAt, expiresAt, createdBy, createdAt, updatedAt);
    }
}
