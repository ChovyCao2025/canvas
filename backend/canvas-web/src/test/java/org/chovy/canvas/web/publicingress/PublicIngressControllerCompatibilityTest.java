package org.chovy.canvas.web.publicingress;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.api.PublicIngressFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class PublicIngressControllerCompatibilityTest {

    @Test
    void exposesAllLegacyPublicIngressRoutesWithCompatibilityEnvelope() {
        RecordingPublicIngressFacade facade = new RecordingPublicIngressFacade();
        WebTestClient client = webClient(facade);

        client.get().uri("/public/marketing-forms/lead-capture")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.publicKey").isEqualTo("lead-capture")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        client.post().uri("/public/marketing-forms/lead-capture/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("response", Map.of("email", "buyer@example.com"),
                        "anonymousId", "anon-1", "idempotencyKey", "idem-1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.accepted").isEqualTo(true)
                .jsonPath("$.data.anonymousId").isEqualTo("anon-1");

        verifyWhatsApp(client, "/public/conversation-webhooks/7/whatsapp");
        verifyWhatsApp(client, "/public/conversations/webhooks/7/whatsapp");
        receiveWhatsApp(client, "/public/conversation-webhooks/7/whatsapp");
        receiveWhatsApp(client, "/public/conversations/webhooks/7/whatsapp");

        client.post().uri("/public/marketing/content/assets/upload-callbacks/7/meta")
                .header("X-Canvas-Asset-Timestamp", "123")
                .header("X-Canvas-Asset-Signature", "asset-sig")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"uploadToken\":\"up-1\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.provider").isEqualTo("meta")
                .jsonPath("$.data.accepted").isEqualTo(true);

        client.post().uri("/public/marketing-monitoring/webhooks/7/ads-health")
                .header("X-Canvas-Monitoring-Timestamp", "456")
                .header("X-Canvas-Monitoring-Signature", "monitoring-sig")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"event\":\"DELIVERY_LAG\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.sourceKey").isEqualTo("ads-health")
                .jsonPath("$.data.accepted").isEqualTo(true);

        assertThat(facade.operations).containsExactly(
                "publicMarketingForm",
                "submitMarketingForm",
                "verifyWhatsApp",
                "verifyWhatsApp",
                "receiveWhatsApp",
                "receiveWhatsApp",
                "receiveAssetUploadCallback",
                "receiveMonitoringWebhook");
    }

    @Test
    void mapsQueryHeadersPathVariablesAndRawBodiesToFacade() {
        RecordingPublicIngressFacade facade = new RecordingPublicIngressFacade();
        WebTestClient client = webClient(facade);

        client.get().uri("/public/conversation-webhooks/42/whatsapp?hub.mode=subscribe&hub.verify_token=token&hub.challenge=challenge-42")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data").isEqualTo("challenge-42");

        assertThat(facade.lastTenantId).isEqualTo(42L);
        assertThat(facade.lastMode).isEqualTo("subscribe");
        assertThat(facade.lastVerifyToken).isEqualTo("token");
        assertThat(facade.lastChallenge).isEqualTo("challenge-42");

        client.post().uri("/public/marketing/content/assets/upload-callbacks/43/tiktok")
                .header("X-Canvas-Asset-Timestamp", "789")
                .header("X-Canvas-Asset-Signature", "asset-signature")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"status\":\"READY\"}")
                .exchange()
                .expectStatus().isOk();

        assertThat(facade.lastTenantId).isEqualTo(43L);
        assertThat(facade.lastProvider).isEqualTo("tiktok");
        assertThat(facade.lastTimestamp).isEqualTo("789");
        assertThat(facade.lastSignature).isEqualTo("asset-signature");
        assertThat(facade.lastRawBody).isEqualTo("{\"status\":\"READY\"}");
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingPublicIngressFacade facade = new RecordingPublicIngressFacade();
        facade.failPublicForm = true;

        webClient(facade)
                .get()
                .uri("/public/marketing-forms/bad-key")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("publicKey is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(PublicIngressFacade facade) {
        return WebTestClient.bindToController(new PublicIngressController(facade)).build();
    }

    private static void verifyWhatsApp(WebTestClient client, String uri) {
        client.get().uri(uri + "?hub.mode=subscribe&hub.verify_token=token&hub.challenge=challenge")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data").isEqualTo("challenge");
    }

    private static void receiveWhatsApp(WebTestClient client, String uri) {
        client.post().uri(uri)
                .header("X-Hub-Signature-256", "sha256=abc")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"entry\":[{\"id\":\"msg-1\"}]}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].provider").isEqualTo("WHATSAPP")
                .jsonPath("$.data[0].accepted").isEqualTo(true);
    }

    private static final class RecordingPublicIngressFacade implements PublicIngressFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastMode;
        private String lastVerifyToken;
        private String lastChallenge;
        private String lastSignature;
        private String lastTimestamp;
        private String lastProvider;
        private String lastRawBody;
        private boolean failPublicForm;

        @Override
        public Map<String, Object> publicMarketingForm(String publicKey) {
            operations.add("publicMarketingForm");
            if (failPublicForm) {
                throw new IllegalArgumentException("publicKey is required");
            }
            return Map.of("publicKey", publicKey, "active", true);
        }

        @Override
        public Map<String, Object> submitMarketingForm(String publicKey, Map<String, Object> payload,
                                                       Map<String, String> headers) {
            operations.add("submitMarketingForm");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("publicKey", publicKey);
            result.put("accepted", true);
            result.put("anonymousId", payload.get("anonymousId"));
            result.put("idempotencyKey", payload.get("idempotencyKey"));
            return result;
        }

        @Override
        public String verifyWhatsApp(Long tenantId, String mode, String verifyToken, String challenge) {
            operations.add("verifyWhatsApp");
            lastTenantId = tenantId;
            lastMode = mode;
            lastVerifyToken = verifyToken;
            lastChallenge = challenge;
            return challenge;
        }

        @Override
        public List<Map<String, Object>> receiveWhatsApp(Long tenantId, String signature, String rawBody) {
            operations.add("receiveWhatsApp");
            lastTenantId = tenantId;
            lastSignature = signature;
            lastRawBody = rawBody;
            return List.of(Map.of("tenantId", tenantId, "provider", "WHATSAPP", "accepted", true));
        }

        @Override
        public Map<String, Object> receiveAssetUploadCallback(Long tenantId, String provider, String timestamp,
                                                              String signature, String rawBody) {
            operations.add("receiveAssetUploadCallback");
            lastTenantId = tenantId;
            lastProvider = provider;
            lastTimestamp = timestamp;
            lastSignature = signature;
            lastRawBody = rawBody;
            return Map.of("tenantId", tenantId, "provider", provider, "accepted", true);
        }

        @Override
        public Map<String, Object> receiveMonitoringWebhook(Long tenantId, String sourceKey, String timestamp,
                                                            String signature, String rawBody) {
            operations.add("receiveMonitoringWebhook");
            lastTenantId = tenantId;
            lastTimestamp = timestamp;
            lastSignature = signature;
            lastRawBody = rawBody;
            return Map.of("tenantId", tenantId, "sourceKey", sourceKey, "accepted", true);
        }
    }
}
