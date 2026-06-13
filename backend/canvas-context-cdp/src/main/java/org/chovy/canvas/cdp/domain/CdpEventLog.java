package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;
import java.util.Map;

public record CdpEventLog(
        Long id,
        Long tenantId,
        Long writeKeyId,
        String messageId,
        String eventType,
        String eventCode,
        String userId,
        String anonymousId,
        String sessionId,
        String deviceId,
        String platform,
        Map<String, Object> sdkContext,
        Map<String, Object> properties,
        String idempotencyKey,
        LocalDateTime eventTime,
        LocalDateTime sentAt,
        LocalDateTime receivedAt,
        String status,
        String errorMessage,
        LocalDateTime createdAt) {

    public static final String ACCEPTED = "ACCEPTED";

    public CdpEventLog withId(Long newId) {
        return new CdpEventLog(newId, tenantId, writeKeyId, messageId, eventType, eventCode, userId, anonymousId,
                sessionId, deviceId, platform, sdkContext, properties, idempotencyKey, eventTime, sentAt, receivedAt,
                status, errorMessage, createdAt);
    }
}
