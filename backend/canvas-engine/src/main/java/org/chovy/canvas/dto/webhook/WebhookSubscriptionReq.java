package org.chovy.canvas.dto.webhook;

import java.util.List;

public record WebhookSubscriptionReq(
        String name,
        String callbackUrl,
        List<String> eventTypes,
        Integer maxAttempts
) {
}
