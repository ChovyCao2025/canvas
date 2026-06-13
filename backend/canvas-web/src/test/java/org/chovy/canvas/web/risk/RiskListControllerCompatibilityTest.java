package org.chovy.canvas.web.risk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.chovy.canvas.risk.api.RiskListFacade;
import org.chovy.canvas.risk.api.RiskListView;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class RiskListControllerCompatibilityTest {

    @Test
    void listListsPreservesLegacyRouteEnvelopeAndUsesTenantHeader() {
        CapturingRiskListFacade facade = new CapturingRiskListFacade(List.of(list(42L, "coupon_abuse")));

        webClient(facade)
                .get()
                .uri("/canvas/risk/lists")
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
                .jsonPath("$.data[0].listKey").isEqualTo("coupon_abuse")
                .jsonPath("$.data[0].listType").isEqualTo("BLACK")
                .jsonPath("$.data[0].subjectType").isEqualTo("USER_ID")
                .jsonPath("$.data[0].status").isEqualTo("ACTIVE")
                .jsonPath("$.data[0].requiresApproval").isEqualTo(true)
                .jsonPath("$.data[0].owner").isEqualTo("risk-ops");

        assertThat(facade.tenantIds).containsExactly(42L);
        assertThat(facade.threadNames).allSatisfy(threadName -> assertThat(threadName).contains("boundedElastic"));
    }

    @Test
    void listListsDefaultsMissingTenantHeaderToRiskControllerTenant() {
        CapturingRiskListFacade facade = new CapturingRiskListFacade(List.of(list(7L, "device_watch")));

        webClient(facade)
                .get()
                .uri("/canvas/risk/lists")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data[0].tenantId").isEqualTo(7)
                .jsonPath("$.data[0].listKey").isEqualTo("device_watch");

        assertThat(facade.tenantIds).containsExactly(7L);
    }

    private static WebTestClient webClient(RiskListFacade facade) {
        return WebTestClient.bindToController(new RiskListController(facade)).build();
    }

    private static RiskListView list(Long tenantId, String listKey) {
        return new RiskListView(
                tenantId,
                listKey,
                "BLACK",
                "USER_ID",
                "ACTIVE",
                true,
                "risk-ops");
    }

    private static final class CapturingRiskListFacade implements RiskListFacade {
        private final List<RiskListView> response;
        private final List<Long> tenantIds = new ArrayList<>();
        private final List<String> threadNames = new ArrayList<>();

        private CapturingRiskListFacade(List<RiskListView> response) {
            this.response = response;
        }

        @Override
        public List<RiskListView> listLists(Long tenantId) {
            tenantIds.add(tenantId);
            threadNames.add(Thread.currentThread().getName());
            return response;
        }
    }
}
