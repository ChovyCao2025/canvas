package org.chovy.canvas.dto.notification;

public record NotificationWebSocketTicketDTO(
        String ticket,
        int expiresInSeconds
) {
}
