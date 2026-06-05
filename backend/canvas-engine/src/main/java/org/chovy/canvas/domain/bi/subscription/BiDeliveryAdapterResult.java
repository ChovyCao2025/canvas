package org.chovy.canvas.domain.bi.subscription;

public record BiDeliveryAdapterResult(
        String status,
        String message,
        String errorMessage
) {
    public static BiDeliveryAdapterResult delivered(String message) {
        return new BiDeliveryAdapterResult("DELIVERED", message, null);
    }

    public static BiDeliveryAdapterResult pending(String message) {
        return new BiDeliveryAdapterResult("PENDING_ADAPTER", message, null);
    }

    public static BiDeliveryAdapterResult failed(String message, String errorMessage) {
        return new BiDeliveryAdapterResult("FAILED", message, errorMessage);
    }
}
