package org.chovy.canvas.dto.webhook;

public record WebhookRotateSecretResp(
        Long subscriptionId,
        String secret,
        String secretPrefix
) {
}
