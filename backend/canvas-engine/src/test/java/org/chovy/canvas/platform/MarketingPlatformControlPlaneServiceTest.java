package org.chovy.canvas.platform;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class MarketingPlatformControlPlaneServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void summaryExposesMarketingMiddlePlatformCapabilitiesAndGlueLanes() {
        MarketingPlatformControlPlaneService service = new MarketingPlatformControlPlaneService(CLOCK);

        MarketingPlatformControlPlaneService.ControlPlaneSummary summary = service.summary(7L);

        assertThat(summary.tenantId()).isEqualTo(7L);
        assertThat(summary.generatedAt()).isEqualTo("2026-06-06T10:00");
        assertThat(summary.overallStatus()).isEqualTo("CONFIGURATION_REQUIRED");
        assertThat(summary.capabilities())
                .extracting(MarketingPlatformControlPlaneService.CapabilityCard::capabilityKey)
                .contains(
                        "campaign-master-ledger",
                        "growth-activity-center",
                        "integration-contract-registry",
                        "journey-orchestration",
                        "content-lifecycle",
                        "marketing-monitoring",
                        "paid-media-activation",
                        "provider-credential-governance");
        assertThat(summary.integrationLanes())
                .extracting(MarketingPlatformControlPlaneService.IntegrationLane::laneKey)
                .contains(
                        "campaign-to-growth-activities",
                        "content-to-journey",
                        "contracts-to-provider-credentials",
                        "search-to-provider-write",
                        "creator-to-provider-write",
                        "dsp-to-provider-write");
        assertThat(summary.integrationAssets())
                .extracting(MarketingPlatformControlPlaneService.IntegrationAsset::assetKey)
                .contains(
                        "marketing-integration-contract-registry",
                        "growth-activity-center",
                        "campaign-master-resource-ledger",
                        "content-release-runtime-resolver",
                        "search-provider-write-gateway",
                        "programmatic-dsp-provider-write-gateway");
        assertThat(summary.readinessGate().status()).isEqualTo("BLOCKED");
        assertThat(summary.readinessGate().productionReady()).isFalse();
        assertThat(summary.readinessGate().blockers())
                .extracting(MarketingPlatformControlPlaneService.ReadinessFinding::itemKey)
                .contains("integration-contract-registry", "marketing-integration-contract-registry");
    }

    @Test
    void summaryPromotesIntegrationContractsWhenProductionEvidenceExists() {
        MarketingPlatformControlPlaneEvidenceProvider evidenceProvider = tenantId -> runtimeEvidence(
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                3,
                0);
        MarketingPlatformControlPlaneService service =
                new MarketingPlatformControlPlaneService(evidenceProvider, CLOCK);

        MarketingPlatformControlPlaneService.ControlPlaneSummary summary = service.summary(7L);

        assertThat(capability(summary, "integration-contract-registry").status()).isEqualTo("LIVE");
        assertThat(integrationAsset(summary, "marketing-integration-contract-registry").status()).isEqualTo("LIVE");
        assertThat(summary.integrationLanes())
                .filteredOn(lane -> lane.laneKey().equals("contracts-to-provider-credentials"))
                .singleElement()
                .satisfies(lane -> assertThat(lane.status()).isEqualTo("GOVERNED"));
        assertThat(capability(summary, "integration-contract-registry").evidence())
                .extracting(MarketingPlatformControlPlaneService.EvidenceSignal::signalKey)
                .contains(
                        "activeIntegrationContracts",
                        "productionIntegrationContracts",
                        "blockedIntegrationContracts",
                        "degradedIntegrationContracts",
                        "freshPassingProductionIntegrationProbes",
                        "freshFailingProductionIntegrationProbes");
    }

    @Test
    void readinessBlocksIntegrationContractsUntilFreshProductionProbesPass() {
        MarketingPlatformControlPlaneEvidenceProvider evidenceProvider = tenantId -> runtimeEvidence(
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                2,
                1);
        MarketingPlatformControlPlaneService service =
                new MarketingPlatformControlPlaneService(evidenceProvider, CLOCK);

        MarketingPlatformControlPlaneService.ControlPlaneSummary summary = service.summary(7L);

        assertThat(capability(summary, "integration-contract-registry").status())
                .isEqualTo("CONFIGURATION_REQUIRED");
        assertThat(integrationAsset(summary, "marketing-integration-contract-registry").status())
                .isEqualTo("CONFIGURATION_REQUIRED");
        assertThat(summary.readinessGate().blockers())
                .anySatisfy(finding -> {
                    assertThat(finding.itemKey()).isEqualTo("integration-contract-registry");
                    assertThat(finding.reason()).contains("record fresh PASS probes for every production integration contract");
                    assertThat(finding.reason()).contains("resolve failing production integration probes");
                });
    }

    @Test
    void readinessBlocksWhenIntegrationContractsAreBlockedOrDegraded() {
        MarketingPlatformControlPlaneEvidenceProvider evidenceProvider = tenantId -> runtimeEvidence(
                2,
                1,
                1,
                1,
                0,
                0,
                0,
                3,
                0);
        MarketingPlatformControlPlaneService service =
                new MarketingPlatformControlPlaneService(evidenceProvider, CLOCK);

        MarketingPlatformControlPlaneService.ControlPlaneSummary summary = service.summary(7L);

        assertThat(capability(summary, "integration-contract-registry").status())
                .isEqualTo("CONFIGURATION_REQUIRED");
        assertThat(summary.readinessGate().blockers())
                .anySatisfy(finding -> {
                    assertThat(finding.itemKey()).isEqualTo("integration-contract-registry");
                    assertThat(finding.reason()).contains("resolve blocked integration contracts");
                    assertThat(finding.reason()).contains("resolve degraded integration contracts");
                });
    }

    @Test
    void readinessBlocksIntegrationContractsWhenOpenProbeOrSloAlertsExist() {
        MarketingPlatformControlPlaneEvidenceProvider evidenceProvider = tenantId -> runtimeEvidence(
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                3,
                0,
                1,
                2);
        MarketingPlatformControlPlaneService service =
                new MarketingPlatformControlPlaneService(evidenceProvider, CLOCK);

        MarketingPlatformControlPlaneService.ControlPlaneSummary summary = service.summary(7L);

        assertThat(capability(summary, "integration-contract-registry").status())
                .isEqualTo("CONFIGURATION_REQUIRED");
        assertThat(integrationAsset(summary, "marketing-integration-contract-registry").status())
                .isEqualTo("CONFIGURATION_REQUIRED");
        assertThat(capability(summary, "integration-contract-registry").gaps())
                .contains(
                        "resolve OPEN integration contract probe alerts",
                        "resolve OPEN integration contract SLO burn-rate alerts");
        assertThat(capability(summary, "integration-contract-registry").evidence())
                .extracting(MarketingPlatformControlPlaneService.EvidenceSignal::signalKey)
                .contains(
                        "openIntegrationContractProbeAlerts",
                        "openIntegrationContractSloAlerts");
        assertThat(summary.readinessGate().blockers())
                .anySatisfy(finding -> {
                    assertThat(finding.itemKey()).isEqualTo("integration-contract-registry");
                    assertThat(finding.reason()).contains("resolve OPEN integration contract probe alerts");
                    assertThat(finding.reason()).contains("resolve OPEN integration contract SLO burn-rate alerts");
                });
    }

    @Test
    void readinessGateIsDegradedWhenRuntimeIsConfiguredButProviderAdaptersRemainApiOnly() {
        MarketingPlatformControlPlaneEvidenceProvider evidenceProvider = tenantId -> runtimeEvidence(
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                3,
                0);
        MarketingPlatformControlPlaneService service =
                new MarketingPlatformControlPlaneService(evidenceProvider, CLOCK);

        MarketingPlatformControlPlaneService.ControlPlaneSummary summary = service.summary(7L);

        assertThat(summary.readinessGate().status()).isEqualTo("DEGRADED");
        assertThat(summary.readinessGate().productionReady()).isTrue();
        assertThat(summary.readinessGate().warnings())
                .extracting(MarketingPlatformControlPlaneService.ReadinessFinding::itemKey)
                .contains("search-marketing-governance", "search-provider-write-gateway");
    }

    @Test
    void searchMarketingCapabilityRoutesToDedicatedWorkbench() {
        MarketingPlatformControlPlaneEvidenceProvider evidenceProvider = tenantId -> runtimeEvidence(
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                3,
                0);
        MarketingPlatformControlPlaneService service =
                new MarketingPlatformControlPlaneService(evidenceProvider, CLOCK);

        MarketingPlatformControlPlaneService.ControlPlaneSummary summary = service.summary(7L);

        assertThat(capability(summary, "search-marketing-governance").route())
                .isEqualTo("/search-marketing");
        assertThat(summary.actionItems())
                .filteredOn(item -> item.capabilityKey().equals("search-marketing-governance"))
                .allSatisfy(item -> assertThat(item.route()).isEqualTo("/search-marketing"));
    }

    @Test
    void campaignLedgerBlocksWhenCampaignsMissLaunchCoverageEvenWithoutBlockedLinks() {
        MarketingPlatformControlPlaneEvidenceProvider evidenceProvider = tenantId -> runtimeEvidence(
                0,
                0,
                0,
                0,
                1,
                2,
                3,
                3,
                0);
        MarketingPlatformControlPlaneService service =
                new MarketingPlatformControlPlaneService(evidenceProvider, CLOCK);

        MarketingPlatformControlPlaneService.ControlPlaneSummary summary = service.summary(7L);

        assertThat(capability(summary, "campaign-master-ledger").status())
                .isEqualTo("CONFIGURATION_REQUIRED");
        assertThat(integrationAsset(summary, "campaign-master-resource-ledger").status())
                .isEqualTo("CONFIGURATION_REQUIRED");
        assertThat(capability(summary, "campaign-master-ledger").gaps())
                .contains(
                        "resolve launch-required campaign resources that are not active",
                        "attach an active PRIMARY launch dependency to every active campaign",
                        "attach an active MEASUREMENT or BI dashboard dependency to every active campaign");
        assertThat(capability(summary, "campaign-master-ledger").evidence())
                .extracting(MarketingPlatformControlPlaneService.EvidenceSignal::signalKey)
                .contains(
                        "campaignsWithInactiveRequiredLinks",
                        "campaignsMissingPrimaryDependency",
                        "campaignsMissingMeasurementDependency");
        assertThat(summary.readinessGate().blockers())
                .anySatisfy(finding -> {
                    assertThat(finding.itemKey()).isEqualTo("campaign-master-ledger");
                    assertThat(finding.reason()).contains("attach an active PRIMARY launch dependency");
                    assertThat(finding.reason()).contains("attach an active MEASUREMENT or BI dashboard dependency");
                });
    }

    @Test
    void growthActivityCenterPromotesWhenActivitiesHaveReadyEvidence() {
        MarketingPlatformControlPlaneEvidenceProvider evidenceProvider = tenantId -> runtimeEvidence(
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                3,
                0,
                0,
                0,
                2,
                3,
                2,
                0);
        MarketingPlatformControlPlaneService service =
                new MarketingPlatformControlPlaneService(evidenceProvider, CLOCK);

        MarketingPlatformControlPlaneService.ControlPlaneSummary summary = service.summary(7L);

        assertThat(capability(summary, "growth-activity-center").status()).isEqualTo("LIVE");
        assertThat(integrationAsset(summary, "growth-activity-center").status()).isEqualTo("LIVE");
        assertThat(summary.integrationLanes())
                .filteredOn(lane -> lane.laneKey().equals("campaign-to-growth-activities"))
                .singleElement()
                .satisfies(lane -> assertThat(lane.status()).isEqualTo("GOVERNED"));
        assertThat(capability(summary, "growth-activity-center").evidence())
                .extracting(MarketingPlatformControlPlaneService.EvidenceSignal::signalKey)
                .contains(
                        "activeGrowthActivities",
                        "growthActivityRewardPools",
                        "readyGrowthActivities",
                        "blockedGrowthActivityReadiness");
    }

    @Test
    void readinessBlocksGrowthActivitiesWhenReadyCoverageIsMissing() {
        MarketingPlatformControlPlaneEvidenceProvider evidenceProvider = tenantId -> runtimeEvidence(
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                3,
                0,
                0,
                0,
                3,
                1,
                1,
                2);
        MarketingPlatformControlPlaneService service =
                new MarketingPlatformControlPlaneService(evidenceProvider, CLOCK);

        MarketingPlatformControlPlaneService.ControlPlaneSummary summary = service.summary(7L);

        assertThat(capability(summary, "growth-activity-center").status())
                .isEqualTo("CONFIGURATION_REQUIRED");
        assertThat(integrationAsset(summary, "growth-activity-center").status())
                .isEqualTo("CONFIGURATION_REQUIRED");
        assertThat(capability(summary, "growth-activity-center").gaps())
                .contains(
                        "complete readiness for every active growth activity",
                        "resolve growth activity readiness blockers");
        assertThat(summary.readinessGate().blockers())
                .anySatisfy(finding -> {
                    assertThat(finding.itemKey()).isEqualTo("growth-activity-center");
                    assertThat(finding.reason()).contains("resolve growth activity readiness blockers");
                });
    }

    private static MarketingPlatformControlPlaneEvidenceProvider.RuntimeEvidence runtimeEvidence(
            long searchFailedWrites,
            long blockedCampaignLinks,
            long blockedIntegrationContracts,
            long degradedIntegrationContracts,
            long campaignsWithInactiveRequiredLinks,
            long campaignsMissingPrimaryDependency,
            long campaignsMissingMeasurementDependency,
            long freshPassingProductionIntegrationProbeCount,
            long freshFailingProductionIntegrationProbeCount) {
        return runtimeEvidence(
                searchFailedWrites,
                blockedCampaignLinks,
                blockedIntegrationContracts,
                degradedIntegrationContracts,
                campaignsWithInactiveRequiredLinks,
                campaignsMissingPrimaryDependency,
                campaignsMissingMeasurementDependency,
                freshPassingProductionIntegrationProbeCount,
                freshFailingProductionIntegrationProbeCount,
                0,
                0,
                2,
                3,
                2,
                0);
    }

    private static MarketingPlatformControlPlaneEvidenceProvider.RuntimeEvidence runtimeEvidence(
            long searchFailedWrites,
            long blockedCampaignLinks,
            long blockedIntegrationContracts,
            long degradedIntegrationContracts,
            long campaignsWithInactiveRequiredLinks,
            long campaignsMissingPrimaryDependency,
            long campaignsMissingMeasurementDependency,
            long freshPassingProductionIntegrationProbeCount,
            long freshFailingProductionIntegrationProbeCount,
            long openIntegrationContractProbeAlertCount,
            long openIntegrationContractSloAlertCount) {
        return runtimeEvidence(
                searchFailedWrites,
                blockedCampaignLinks,
                blockedIntegrationContracts,
                degradedIntegrationContracts,
                campaignsWithInactiveRequiredLinks,
                campaignsMissingPrimaryDependency,
                campaignsMissingMeasurementDependency,
                freshPassingProductionIntegrationProbeCount,
                freshFailingProductionIntegrationProbeCount,
                openIntegrationContractProbeAlertCount,
                openIntegrationContractSloAlertCount,
                2,
                3,
                2,
                0);
    }

    private static MarketingPlatformControlPlaneEvidenceProvider.RuntimeEvidence runtimeEvidence(
            long searchFailedWrites,
            long blockedCampaignLinks,
            long blockedIntegrationContracts,
            long degradedIntegrationContracts,
            long campaignsWithInactiveRequiredLinks,
            long campaignsMissingPrimaryDependency,
            long campaignsMissingMeasurementDependency,
            long freshPassingProductionIntegrationProbeCount,
            long freshFailingProductionIntegrationProbeCount,
            long openIntegrationContractProbeAlertCount,
            long openIntegrationContractSloAlertCount,
            long activeGrowthActivityCount,
            long growthActivityRewardPoolCount,
            long readyGrowthActivityCount,
            long blockedGrowthActivityReadinessCount) {
        return new MarketingPlatformControlPlaneEvidenceProvider.RuntimeEvidence(
                3,
                2,
                5,
                1,
                1,
                2,
                1,
                1,
                1,
                1,
                4,
                12,
                0,
                searchFailedWrites,
                9,
                0,
                0,
                7,
                0,
                0,
                2,
                5,
                5,
                blockedCampaignLinks,
                campaignsWithInactiveRequiredLinks,
                campaignsMissingPrimaryDependency,
                campaignsMissingMeasurementDependency,
                4,
                3,
                blockedIntegrationContracts,
                degradedIntegrationContracts,
                freshPassingProductionIntegrationProbeCount,
                freshFailingProductionIntegrationProbeCount,
                openIntegrationContractProbeAlertCount,
                openIntegrationContractSloAlertCount,
                activeGrowthActivityCount,
                growthActivityRewardPoolCount,
                readyGrowthActivityCount,
                blockedGrowthActivityReadinessCount);
    }

    private static MarketingPlatformControlPlaneService.CapabilityCard capability(
            MarketingPlatformControlPlaneService.ControlPlaneSummary summary,
            String capabilityKey) {
        return summary.capabilities().stream()
                .filter(capability -> capability.capabilityKey().equals(capabilityKey))
                .findFirst()
                .orElseThrow();
    }

    private static MarketingPlatformControlPlaneService.IntegrationAsset integrationAsset(
            MarketingPlatformControlPlaneService.ControlPlaneSummary summary,
            String assetKey) {
        return summary.integrationAssets().stream()
                .filter(asset -> asset.assetKey().equals(assetKey))
                .findFirst()
                .orElseThrow();
    }
}
