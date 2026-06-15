package org.chovy.canvas.web.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.chovy.canvas.canvas.api.MarketingPlatformControlPlaneFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class MarketingPlatformControlPlaneControllerCompatibilityTest {

    @Test
    void controlPlaneRoutePreservesLegacyEnvelopeAndDefaultsTenant() {
        RecordingMarketingPlatformControlPlaneFacade facade = new RecordingMarketingPlatformControlPlaneFacade();

        webClient(facade)
                .get()
                .uri("/canvas/marketing-platform/control-plane")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.tenantId").isEqualTo(0)
                .jsonPath("$.data.generatedAt").isEqualTo("2026-06-15T08:37:00")
                .jsonPath("$.data.overallStatus").isEqualTo("API_ONLY")
                .jsonPath("$.data.capabilityCount").isEqualTo(2)
                .jsonPath("$.data.liveCapabilityCount").isEqualTo(1)
                .jsonPath("$.data.actionItemCount").isEqualTo(1)
                .jsonPath("$.data.capabilities[0].capabilityKey").isEqualTo("journey-orchestration")
                .jsonPath("$.data.capabilities[0].status").isEqualTo("LIVE")
                .jsonPath("$.data.capabilities[0].evidence[0].signalKey").isEqualTo("publishedJourneys")
                .jsonPath("$.data.capabilities[1].capabilityKey").isEqualTo("search-marketing-governance")
                .jsonPath("$.data.integrationLanes[0].laneKey").isEqualTo("audience-to-paid-media")
                .jsonPath("$.data.integrationAssets[0].assetKey").isEqualTo("paid-media-audience-sync")
                .jsonPath("$.data.readinessGate.status").isEqualTo("BLOCKED")
                .jsonPath("$.data.readinessGate.productionReady").isEqualTo(false)
                .jsonPath("$.data.actionItems[0].capabilityKey").isEqualTo("search-marketing-governance");

        assertThat(facade.lastTenantId).isZero();
    }

    @Test
    void controlPlaneRouteUsesTenantHeaderForSummaryScope() {
        RecordingMarketingPlatformControlPlaneFacade facade = new RecordingMarketingPlatformControlPlaneFacade();

        webClient(facade)
                .get()
                .uri("/canvas/marketing-platform/control-plane")
                .header("X-Tenant-Id", "42")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(42);

        assertThat(facade.lastTenantId).isEqualTo(42L);
    }

    private static WebTestClient webClient(MarketingPlatformControlPlaneFacade facade) {
        return WebTestClient.bindToController(new MarketingPlatformControlPlaneController(facade)).build();
    }

    private static final class RecordingMarketingPlatformControlPlaneFacade
            implements MarketingPlatformControlPlaneFacade {
        private Long lastTenantId;

        @Override
        public ControlPlaneSummaryView summary(Long tenantId) {
            lastTenantId = tenantId;
            return new ControlPlaneSummaryView(
                    tenantId,
                    "2026-06-15T08:37:00",
                    "API_ONLY",
                    2,
                    1,
                    1,
                    List.of(
                            new CapabilityCardView(
                                    "journey-orchestration",
                                    "Journey Orchestration",
                                    "automation",
                                    "LIVE",
                                    "/canvas",
                                    "/canvas",
                                    "operator-facing",
                                    List.of("tenant-scoped journeys"),
                                    List.of(),
                                    List.of(new EvidenceSignalView(
                                            "publishedJourneys",
                                            "Published journeys",
                                            3L,
                                            "PRESENT"))),
                            new CapabilityCardView(
                                    "search-marketing-governance",
                                    "Search Marketing Governance",
                                    "search",
                                    "API_ONLY",
                                    "/search-marketing",
                                    "/canvas/search-marketing",
                                    "api-only",
                                    List.of("keyword portfolio"),
                                    List.of("operator workbench remains provider-specific"),
                                    List.of())),
                    List.of(new IntegrationLaneView(
                            "audience-to-paid-media",
                            "CDP Audiences To Paid Media",
                            "journey-orchestration",
                            "paid-media-activation",
                            "CONFIGURATION_REQUIRED",
                            List.of("consent gate"))),
                    List.of(new IntegrationAssetView(
                            "paid-media-audience-sync",
                            "Paid Media Audience Sync",
                            "OUTBOUND_SYNC",
                            "paid-media-activation",
                            "PAID_MEDIA",
                            "CONFIGURATION_REQUIRED",
                            "/canvas/paid-media/audience-sync",
                            "active provider credential",
                            0L,
                            0L,
                            List.of("consent gate"),
                            List.of("enable paid-media destination"),
                            List.of())),
                    new ReadinessGateView(
                            "BLOCKED",
                            false,
                            1,
                            0,
                            List.of(new ReadinessFindingView(
                                    "BLOCKER",
                                    "CAPABILITY",
                                    "search-marketing-governance",
                                    "Complete capability configuration",
                                    "/search-marketing",
                                    "operator workbench remains provider-specific")),
                            List.of()),
                    List.of(new ActionItemView(
                            "HIGH",
                            "search-marketing-governance",
                            "Wire provider write clients",
                            "/search-marketing",
                            "operator workbench remains provider-specific")));
        }
    }
}
