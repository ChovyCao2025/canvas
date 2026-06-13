package org.chovy.canvas.web.risk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.chovy.canvas.risk.api.RiskSceneFacade;
import org.chovy.canvas.risk.api.RiskSceneView;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class RiskSceneControllerCompatibilityTest {

    @Test
    void listScenesPreservesLegacyRouteEnvelopeAndUsesTenantHeader() {
        CapturingRiskSceneFacade facade = new CapturingRiskSceneFacade(List.of(scene(42L)));

        webClient(facade)
                .get()
                .uri("/canvas/risk/scenes")
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
                .jsonPath("$.data[0].displayName").isEqualTo("营销权益风控")
                .jsonPath("$.data[0].eventSchemaKey").isEqualTo("risk.marketing.benefit.v1")
                .jsonPath("$.data[0].status").isEqualTo("ACTIVE")
                .jsonPath("$.data[0].defaultMode").isEqualTo("ENFORCE")
                .jsonPath("$.data[0].failPolicy").isEqualTo("FAIL_REVIEW")
                .jsonPath("$.data[0].latencyBudgetMs").isEqualTo(50)
                .jsonPath("$.data[0].owner").isEqualTo("risk-ops");

        assertThat(facade.tenantIds).containsExactly(42L);
        assertThat(facade.threadNames).allSatisfy(threadName -> assertThat(threadName).contains("boundedElastic"));
    }

    @Test
    void listScenesDefaultsMissingTenantHeaderToRiskControllerTenant() {
        CapturingRiskSceneFacade facade = new CapturingRiskSceneFacade(List.of(scene(7L)));

        webClient(facade)
                .get()
                .uri("/canvas/risk/scenes")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data[0].tenantId").isEqualTo(7);

        assertThat(facade.tenantIds).containsExactly(7L);
    }

    private static WebTestClient webClient(RiskSceneFacade facade) {
        return WebTestClient.bindToController(new RiskSceneController(facade)).build();
    }

    private static RiskSceneView scene(Long tenantId) {
        return new RiskSceneView(
                tenantId,
                "MARKETING_BENEFIT_ISSUE",
                "营销权益风控",
                "risk.marketing.benefit.v1",
                "ACTIVE",
                "ENFORCE",
                "FAIL_REVIEW",
                50,
                "risk-ops");
    }

    private static final class CapturingRiskSceneFacade implements RiskSceneFacade {
        private final List<RiskSceneView> response;
        private final List<Long> tenantIds = new ArrayList<>();
        private final List<String> threadNames = new ArrayList<>();

        private CapturingRiskSceneFacade(List<RiskSceneView> response) {
            this.response = response;
        }

        @Override
        public List<RiskSceneView> listScenes(Long tenantId) {
            tenantIds.add(tenantId);
            threadNames.add(Thread.currentThread().getName());
            return response;
        }
    }
}
