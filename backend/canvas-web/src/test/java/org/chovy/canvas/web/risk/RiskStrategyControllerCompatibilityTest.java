package org.chovy.canvas.web.risk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.chovy.canvas.risk.api.RiskStrategyFacade;
import org.chovy.canvas.risk.api.RiskStrategyView;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class RiskStrategyControllerCompatibilityTest {

    @Test
    void listStrategiesPreservesLegacyRouteEnvelopeTenantHeaderSceneKeyAndBoundedElasticExecution() {
        CapturingRiskStrategyFacade facade = new CapturingRiskStrategyFacade(
                List.of(strategy(42L, "MARKETING_BENEFIT_ISSUE", "benefit_default")));

        webClient(facade)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/risk/strategies")
                        .queryParam("sceneKey", "MARKETING_BENEFIT_ISSUE")
                        .build())
                .header("X-Tenant-Id", "42")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data[0].tenantId").isEqualTo(42)
                .jsonPath("$.data[0].sceneKey").isEqualTo("MARKETING_BENEFIT_ISSUE")
                .jsonPath("$.data[0].strategyKey").isEqualTo("benefit_default")
                .jsonPath("$.data[0].name").isEqualTo("Benefit Default")
                .jsonPath("$.data[0].status").isEqualTo("ACTIVE")
                .jsonPath("$.data[0].activeVersion").isEqualTo(3)
                .jsonPath("$.data[0].draftVersion").isEqualTo(4)
                .jsonPath("$.data[0].riskLevel").isEqualTo("MEDIUM")
                .jsonPath("$.data[0].owner").isEqualTo("risk-ops");

        assertThat(facade.requests).containsExactly(new Request(42L, "MARKETING_BENEFIT_ISSUE"));
        assertThat(facade.threadNames).allSatisfy(threadName -> assertThat(threadName).contains("boundedElastic"));
    }

    @Test
    void listStrategiesDefaultsMissingTenantHeaderToRiskControllerTenant() {
        CapturingRiskStrategyFacade facade = new CapturingRiskStrategyFacade(
                List.of(strategy(7L, "DEVICE_FINGERPRINT", "device_watch")));

        webClient(facade)
                .get()
                .uri("/canvas/risk/strategies")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data[0].tenantId").isEqualTo(7)
                .jsonPath("$.data[0].strategyKey").isEqualTo("device_watch");

        assertThat(facade.requests).containsExactly(new Request(7L, null));
    }

    private static WebTestClient webClient(RiskStrategyFacade facade) {
        return WebTestClient.bindToController(new RiskStrategyController(facade)).build();
    }

    private static RiskStrategyView strategy(Long tenantId, String sceneKey, String strategyKey) {
        return new RiskStrategyView(
                tenantId,
                sceneKey,
                strategyKey,
                "Benefit Default",
                "ACTIVE",
                3,
                4,
                "MEDIUM",
                "risk-ops");
    }

    private record Request(Long tenantId, String sceneKey) {
    }

    private static final class CapturingRiskStrategyFacade implements RiskStrategyFacade {
        private final List<RiskStrategyView> response;
        private final List<Request> requests = new ArrayList<>();
        private final List<String> threadNames = new ArrayList<>();

        private CapturingRiskStrategyFacade(List<RiskStrategyView> response) {
            this.response = response;
        }

        @Override
        public List<RiskStrategyView> listStrategies(Long tenantId, String sceneKey) {
            requests.add(new Request(tenantId, sceneKey));
            threadNames.add(Thread.currentThread().getName());
            return response;
        }
    }
}
