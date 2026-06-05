package org.chovy.canvas.domain.cdp;

import java.util.Map;

public record WebhookDeliveryPayload(
        String schemaVersion,
        String eventType,
        String deliveryId,
        Map<String, Object> data
) {
    public static WebhookDeliveryPayload of(String eventType, String deliveryId, Map<String, Object> data) {
        return new WebhookDeliveryPayload("2026-06-03", eventType, deliveryId, data);
    }
}
