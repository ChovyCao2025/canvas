package org.chovy.canvas.cdp.api;

import java.time.OffsetDateTime;
import java.util.Map;

public record CdpTrackEventCommand(
        String messageId,
        String type,
        String event,
        String userId,
        String anonymousId,
        String idempotencyKey,
        Map<String, Object> properties,
        Map<String, Object> context,
        OffsetDateTime timestamp,
        OffsetDateTime sentAt) {
}
