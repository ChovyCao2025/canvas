package org.chovy.canvas.platform.application;

import org.chovy.canvas.platform.api.MarketingPlatformControlPlaneEvidenceProvider;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 覆盖营销平台控制面汇总、集成契约证据和就绪门禁。
 */
class MarketingPlatformControlPlaneServiceTest {

    /**
     * 测试用固定时钟，保证生成时间稳定。
     */
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    /**
     * 验证空证据下控制面能力齐全且整体被配置缺口阻塞。
     */
    @Test
    void summaryExposesCoreControlPlaneCapabilitiesAndBlocksEmptyEvidence() {
        MarketingPlatformControlPlaneService service =
                new MarketingPlatformControlPlaneService(MarketingPlatformControlPlaneEvidenceProvider.empty(), CLOCK);

        var summary = service.summary(7L);

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
        assertThat(summary.readinessGate().status()).isEqualTo("BLOCKED");
        assertThat(summary.readinessGate().productionReady()).isFalse();
        assertThat(summary.readinessGate().blockers())
                .extracting(MarketingPlatformControlPlaneService.ReadinessFinding::itemKey)
                .contains("integration-contract-registry");
    }

    /**
     * 验证生产集成契约只有在具备新近通过探针时才被提升为可用。
     */
    @Test
    void summaryPromotesIntegrationContractsOnlyWithFreshProductionEvidence() {
        MarketingPlatformControlPlaneEvidenceProvider evidenceProvider = tenantId ->
                new MarketingPlatformControlPlaneEvidenceProvider.RuntimeEvidence(
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0,
                        3, 3, 0, 0, 3, 0, 0, 0,
                        0, 0, 0, 0);
        MarketingPlatformControlPlaneService service =
                new MarketingPlatformControlPlaneService(evidenceProvider, CLOCK);

        var summary = service.summary(null);

        assertThat(summary.tenantId()).isZero();
        assertThat(capability(summary, "integration-contract-registry").status()).isEqualTo("LIVE");
        assertThat(summary.readinessGate().warnings())
                .extracting(MarketingPlatformControlPlaneService.ReadinessFinding::itemKey)
                .contains("search-marketing-governance");
    }

    /**
     * 验证新近失败探针会阻塞集成契约就绪。
     */
    @Test
    void readinessBlocksIntegrationContractsWhenFreshProductionProbesFail() {
        MarketingPlatformControlPlaneEvidenceProvider evidenceProvider = tenantId ->
                new MarketingPlatformControlPlaneEvidenceProvider.RuntimeEvidence(
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0,
                        3, 3, 0, 0, 2, 1, 0, 0,
                        0, 0, 0, 0);
        MarketingPlatformControlPlaneService service =
                new MarketingPlatformControlPlaneService(evidenceProvider, CLOCK);

        var summary = service.summary(7L);

        assertThat(capability(summary, "integration-contract-registry").status())
                .isEqualTo("CONFIGURATION_REQUIRED");
        assertThat(capability(summary, "integration-contract-registry").gaps())
                .contains("record fresh PASS probes for every production integration contract")
                .contains("resolve failing production integration probes");
    }

    /**
     * 按能力键从汇总中查找能力卡片。
     *
     * @param summary 控制面汇总
     * @param capabilityKey 能力稳定键
     * @return 匹配的能力卡片
     */
    private static MarketingPlatformControlPlaneService.CapabilityCard capability(
            MarketingPlatformControlPlaneService.ControlPlaneSummary summary,
            String capabilityKey) {
        return summary.capabilities().stream()
                .filter(capability -> capability.capabilityKey().equals(capabilityKey))
                .findFirst()
                .orElseThrow();
    }
}
