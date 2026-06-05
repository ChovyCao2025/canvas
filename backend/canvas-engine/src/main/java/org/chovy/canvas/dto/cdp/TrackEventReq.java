package org.chovy.canvas.dto.cdp;

import java.time.OffsetDateTime;
import java.util.Map;

public record TrackEventReq(
        String messageId,
        String type,
        String event,
        String userId,
        String anonymousId,
        String idempotencyKey,
        Map<String, Object> properties,
        Map<String, Object> context,
        OffsetDateTime timestamp,
        OffsetDateTime sentAt
) {
}
