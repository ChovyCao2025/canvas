package org.chovy.canvas.canvas.application;

import java.time.LocalDateTime;
import java.util.List;

import org.chovy.canvas.canvas.api.MarketingPlatformControlPlaneFacade;
import org.springframework.stereotype.Service;

@Service
public class MarketingPlatformControlPlaneApplicationService implements MarketingPlatformControlPlaneFacade {

    @Override
    public ControlPlaneSummaryView summary(Long tenantId) {
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        return new ControlPlaneSummaryView(
                scopedTenantId,
                LocalDateTime.now().withNano(0).toString(),
                "CONFIGURATION_REQUIRED",
                2,
                1,
                1,
                List.of(
                        new CapabilityCardView(
                                "journey-orchestration",
                                "Journey Orchestration",
                                "canvas",
                                "LIVE",
                                "/canvas",
                                "/canvas",
                                "CONTROL_PLANE",
                                List.of("routes available"),
                                List.of(),
                                List.of(new EvidenceSignalView("route-count", "Routes", 1L, "PASS"))),
                        new CapabilityCardView(
                                "integration-contract-registry",
                                "Integration Contracts",
                                "platform",
                                "CONFIGURATION_REQUIRED",
                                "/canvas/marketing-platform",
                                "/canvas/marketing-platform",
                                "CONTROL_PLANE",
                                List.of(),
                                List.of("record fresh PASS probes"),
                                List.of())),
                List.of(new IntegrationLaneView(
                        "journey-to-monitoring",
                        "Journey to Monitoring",
                        "journey-orchestration",
                        "marketing-monitoring",
                        "DEGRADED",
                        List.of("contract-gate"))),
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
                                "integration-contract-registry",
                                "Missing production evidence",
                                "/canvas/marketing-platform",
                                "record fresh PASS probes")),
                        List.of()),
                List.of(new ActionItemView(
                        "HIGH",
                        "integration-contract-registry",
                        "Configure production contracts",
                        "/canvas/marketing-platform",
                        "record fresh PASS probes")));
    }
}
