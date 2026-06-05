package org.chovy.canvas.dto.webhook;

import java.time.LocalDateTime;
import java.util.List;

public record WebhookSubscriptionDTO(
        Long id,
        String name,
        String callbackUrl,
        String secretPrefix,
        List<String> eventTypes,
        String status,
        Integer maxAttempts,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
