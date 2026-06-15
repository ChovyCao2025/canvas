package org.chovy.canvas.cdp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWebhookFacade;
import org.junit.jupiter.api.Test;

class CdpWebhookApplicationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-14T04:20:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void managesWebhookSubscriptionLifecycleDeliveriesAndSecretRotationWithinTenant() {
        CdpWebhookFacade service = new CdpWebhookApplicationService(CLOCK);

        Map<String, Object> created = service.create(7L, Map.of(
                "name", "Order Paid Hook",
                "callbackUrl", " https://hooks.example.test/order-paid ",
                "eventTypes", List.of("order.paid", "coupon.issued"),
                "maxAttempts", 5), "operator-1");
        Long id = (Long) created.get("id");

        Map<String, Object> listed = service.list(7L);
        Map<String, Object> updated = service.update(7L, id, Map.of(
                "name", "Order Paid Hook v2",
                "callbackUrl", "https://hooks.example.test/order-paid-v2",
                "eventTypes", List.of("order.paid"),
                "maxAttempts", 2), "operator-2");
        Map<String, Object> paused = service.pause(7L, id, "operator-3");
        Map<String, Object> resumed = service.resume(7L, id, "operator-4");
        Map<String, Object> rotated = service.rotateSecret(7L, id, "operator-5");
        Map<String, Object> tested = service.testDelivery(7L, id, "operator-6");
        Map<String, Object> deliveries = service.deliveries(7L, id, null);
        Map<String, Object> disabled = service.disable(7L, id, "operator-7");

        assertThat(created).containsEntry("id", 1L)
                .containsEntry("tenantId", 7L)
                .containsEntry("name", "Order Paid Hook")
                .containsEntry("callbackUrl", "https://hooks.example.test/order-paid")
                .containsEntry("status", "ACTIVE")
                .containsEntry("maxAttempts", 5)
                .containsEntry("createdBy", "operator-1")
                .containsKey("secretPrefix");
        assertThat(created.get("eventTypes")).isEqualTo(List.of("order.paid", "coupon.issued"));
        assertThat(listed).containsEntry("total", 1L);
        assertThat((List<?>) listed.get("records")).hasSize(1);
        assertThat(updated).containsEntry("name", "Order Paid Hook v2")
                .containsEntry("callbackUrl", "https://hooks.example.test/order-paid-v2")
                .containsEntry("maxAttempts", 2)
                .containsEntry("updatedBy", "operator-2");
        assertThat(paused).containsEntry("status", "PAUSED").containsEntry("updatedBy", "operator-3");
        assertThat(resumed).containsEntry("status", "ACTIVE").containsEntry("updatedBy", "operator-4");
        assertThat(rotated).containsEntry("subscriptionId", id)
                .containsEntry("rotatedBy", "operator-5")
                .containsKey("secret")
                .containsKey("secretPrefix");
        assertThat(tested).containsEntry("subscriptionId", id)
                .containsEntry("eventType", "webhook.test")
                .containsEntry("status", "QUEUED")
                .containsEntry("triggeredBy", "operator-6");
        assertThat(deliveries).containsEntry("total", 1L);
        assertThat((List<?>) deliveries.get("records")).hasSize(1);
        assertThat(disabled).containsEntry("status", "DISABLED")
                .containsEntry("updatedBy", "operator-7");

        assertThat(service.list(8L)).containsEntry("total", 0L);
    }

    @Test
    void validationDefaultsAndTenantIsolationAreStable() {
        CdpWebhookFacade service = new CdpWebhookApplicationService(CLOCK);
        Map<String, Object> created = service.create(null, Map.of(
                "name", "Default Tenant Hook",
                "callbackUrl", "https://hooks.example.test/default",
                "eventTypes", List.of("webhook.test")), "");
        Long id = (Long) created.get("id");

        assertThat(created).containsEntry("tenantId", 0L)
                .containsEntry("createdBy", "system")
                .containsEntry("maxAttempts", 3);
        assertThat(service.deliveries(null, id, null)).containsEntry("total", 0L);
        assertThatThrownBy(() -> service.create(7L, Map.of("callbackUrl", "https://example.test"), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name is required");
        assertThatThrownBy(() -> service.create(7L, Map.of(
                "name", "Bad Hook",
                "callbackUrl", "http://example.test",
                "eventTypes", List.of("webhook.test")), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("callbackUrl must start with https://");
        assertThatThrownBy(() -> service.update(8L, id, Map.of(
                "name", "Wrong Tenant",
                "callbackUrl", "https://hooks.example.test/wrong",
                "eventTypes", List.of("webhook.test")), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Webhook subscription not found");
    }
}
