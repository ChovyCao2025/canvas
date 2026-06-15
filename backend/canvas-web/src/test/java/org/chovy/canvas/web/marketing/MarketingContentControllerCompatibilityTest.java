package org.chovy.canvas.web.marketing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MarketingContentFacade;
import org.chovy.canvas.marketing.application.MarketingContentApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class MarketingContentControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    @Test
    void exposesAllLegacyMarketingContentRouteShapesThroughFinalController() {
        WebTestClient client = webClient(new MarketingContentApplicationService());

        List<RouteProbe> probes = List.of(
                get("/asset-folders"),
                post("/asset-folders", Map.of("folderName", "Hero Assets")),
                get("/assets?keyword=hero&assetType=image&status=active"),
                post("/assets", Map.of("assetName", "Hero", "assetType", "image")),
                post("/assets/upload-intents", Map.of("fileName", "hero.png", "assetType", "image")),
                post("/assets/upload-intents/expire-stale", Map.of("olderThanMinutes", 1)),
                post("/assets/asset-1/status", Map.of("status", "inactive")),
                get("/templates?keyword=promo&channel=email&status=draft"),
                post("/templates", Map.of("templateName", "Promo", "channel", "email", "body", "Hi {{name}}")),
                post("/templates/template-1/preview", Map.of("name", "Ada")),
                post("/templates/template-1/status", Map.of("status", "active")),
                get("/entries?keyword=promo&contentType=email&status=draft"),
                post("/entries", Map.of("title", "Promo", "contentType", "email")),
                post("/entries/entry-1/publish", Map.of("reason", "ready")),
                post("/entries/entry-1/archive", Map.of("reason", "done")),
                post("/releases/validate", Map.of("sourceType", "entry", "sourceKey", "entry-1")),
                post("/releases/publish", Map.of("sourceType", "entry", "sourceKey", "entry-1", "channel", "email")),
                get("/releases?sourceType=entry&sourceKey=entry-1&status=published"),
                post("/releases/release-1/resolve", Map.of("externalStatus", "delivered")),
                post("/releases/release-1/rollback", Map.of("reason", "incorrect audience")),
                get("/audit-events?targetType=release&targetKey=release-1&limit=20"));

        for (RouteProbe probe : probes) {
            probe.exchange(client)
                    .expectStatus().isOk()
                    .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(0)
                    .jsonPath("$.message").isEqualTo("success")
                    .jsonPath("$.errorCode").doesNotExist()
                    .jsonPath("$.traceId").doesNotExist();
        }
    }

    @Test
    void missingHeadersUseCompatibilityDefaultsAndBadRequestsMapToApi001() {
        RecordingMarketingContentFacade facade = new RecordingMarketingContentFacade();

        webClient(facade)
                .post()
                .uri("/marketing/content/assets")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("assetName", "Hero"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.updatedBy").isEqualTo(DEFAULT_ACTOR);

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(DEFAULT_ACTOR);

        facade.failCreateAsset = true;

        webClient(facade)
                .post()
                .uri("/marketing/content/assets")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("assetName", ""))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("assetName is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(MarketingContentFacade facade) {
        return WebTestClient.bindToController(new MarketingContentController(facade)).build();
    }

    private static RouteProbe get(String path) {
        return new RouteProbe("GET", path, Map.of());
    }

    private static RouteProbe post(String path, Map<String, Object> body) {
        return new RouteProbe("POST", path, body);
    }

    private record RouteProbe(String method, String path, Map<String, Object> body) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            if ("GET".equals(method)) {
                return client.get().uri("/marketing/content" + path).exchange();
            }
            return client.post()
                    .uri("/marketing/content" + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchange();
        }
    }

    private static final class RecordingMarketingContentFacade extends MarketingContentApplicationService {
        private Long lastTenantId;
        private String lastActor;
        private boolean failCreateAsset;

        @Override
        public Map<String, Object> createAsset(Long tenantId, Map<String, Object> payload, String actor) {
            if (failCreateAsset) {
                throw new IllegalArgumentException("assetName is required");
            }
            lastTenantId = tenantId;
            lastActor = actor;
            return Map.of("tenantId", tenantId, "updatedBy", actor);
        }
    }
}
