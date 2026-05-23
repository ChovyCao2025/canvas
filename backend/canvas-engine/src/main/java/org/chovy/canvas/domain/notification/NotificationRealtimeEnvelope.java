package org.chovy.canvas.domain.notification;

import org.chovy.canvas.dto.notification.NotificationRealtimePayload;

public record NotificationRealtimeEnvelope(
        String originId,
        String userId,
        NotificationRealtimePayload payload
) {
}
