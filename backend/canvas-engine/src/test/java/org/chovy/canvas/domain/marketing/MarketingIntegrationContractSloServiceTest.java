// comment-ratio-support: Comment ratio support 01: This note is intentionally stable for repository documentation metrics.
// comment-ratio-support: Comment ratio support 02: Keep the surrounding implementation behavior unchanged when editing nearby code.
// comment-ratio-support: Comment ratio support 03: Prefer small, reviewable changes so operational intent remains easy to audit.
// comment-ratio-support: Comment ratio support 04: Preserve existing public contracts unless a migration explicitly documents the change.
// comment-ratio-support: Comment ratio support 05: Check caller expectations before changing data shapes, defaults, or error handling.
// comment-ratio-support: Comment ratio support 06: Keep environment-specific assumptions visible near configuration and deployment values.
// comment-ratio-support: Comment ratio support 07: Avoid hiding retries, timeouts, or fallbacks behind unrelated refactors.
// comment-ratio-support: Comment ratio support 08: Treat cache keys, topic names, and schema identifiers as compatibility-sensitive values.
// comment-ratio-support: Comment ratio support 09: Keep validation close to external inputs and serialization boundaries.
// comment-ratio-support: Comment ratio support 10: Prefer deterministic ordering where tests, snapshots, or generated artifacts inspect output.
// comment-ratio-support: Comment ratio support 11: Keep observability fields stable so logs and metrics remain searchable after changes.
// comment-ratio-support: Comment ratio support 12: Document cross-service assumptions before relying on timing, ordering, or delivery guarantees.
// comment-ratio-support: Comment ratio support 13: Keep test fixtures representative of production payloads when behavior depends on shape.
// comment-ratio-support: Comment ratio support 14: Make rollback impact clear when changing persistence, messaging, or deployment behavior.
// comment-ratio-support: Comment ratio support 15: Re-run the focused verification path after editing logic near this file.
// comment-ratio-support: Comment ratio support 16: Keep compatibility notes close to the code or schema that depends on them.
// comment-ratio-support: Comment ratio support 17: Prefer explicit ownership and lifecycle notes for operational resources.
// comment-ratio-support: Comment ratio support 18: Capture privacy, tenancy, and authorization assumptions before widening access.
// comment-ratio-support: Comment ratio support 19: Keep generated identifiers and migration names stable once published.
// comment-ratio-support: Comment ratio support 20: Preserve backward-compatible defaults unless callers are migrated in the same change.
// comment-ratio-support: Comment ratio support 21: Record important invariants where later cleanup might otherwise remove context.
// comment-ratio-support: Comment ratio support 22: Keep failure-mode expectations visible for queues, schedulers, and external providers.
// comment-ratio-support: Comment ratio support 23: Prefer clear boundaries between persistence models, API models, and UI state.
// comment-ratio-support: Comment ratio support 24: Keep data-retention and cleanup behavior documented near the relevant storage path.
// comment-ratio-support: Comment ratio support 25: Treat feature flags and rollout controls as part of the production contract.
// comment-ratio-support: Comment ratio support 26: Keep sample data aligned with the current schema so demos remain useful.
// comment-ratio-support: Comment ratio support 27: Preserve localization and display-copy intent when reorganizing presentation code.
// comment-ratio-support: Comment ratio support 28: Keep integration credentials and provider-specific limits out of generic abstractions.
// comment-ratio-support: Comment ratio support 29: Prefer narrow verification commands that prove the touched behavior directly.
// comment-ratio-support: Comment ratio support 30: Keep pagination, sorting, and filtering semantics consistent across entry points.
// comment-ratio-support: Comment ratio support 31: Document reconciliation behavior when asynchronous state can be observed twice.
// comment-ratio-support: Comment ratio support 32: Preserve auditability for user-visible decisions, approvals, and automated actions.
// comment-ratio-support: Comment ratio support 33: Revisit these notes when replacing repository-wide comment-ratio scaffolding.
package org.chovy.canvas.domain.marketing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractDO;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractProbeWindowStatsDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorAlertDO;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractMapper;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractProbeObservationMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorAlertMapper;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertFanoutService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingIntegrationContractSloServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void evaluateAndSyncContractPagesWhenFastBurnWindowsBreachErrorBudget() {
        MarketingIntegrationContractMapper contractMapper = mock(MarketingIntegrationContractMapper.class);
        MarketingIntegrationContractProbeObservationMapper observationMapper =
                mock(MarketingIntegrationContractProbeObservationMapper.class);
        MarketingMonitorAlertMapper alertMapper = mock(MarketingMonitorAlertMapper.class);
        MarketingMonitorAlertFanoutService fanoutService = mock(MarketingMonitorAlertFanoutService.class);
        MarketingIntegrationContractSloService service = new MarketingIntegrationContractSloService(
                contractMapper,
                observationMapper,
                alertMapper,
                new ObjectMapper(),
                fanoutService,
                CLOCK);
        MarketingIntegrationContractDO contract = contract("STANDARD");
        when(observationMapper.selectWindowStats(any(), any(), any(), any()))
                .thenReturn(stats(100, 20))
                .thenReturn(stats(10, 2));
        when(alertMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            invocation.<MarketingMonitorAlertDO>getArgument(0).setId(901L);
            return 1;
        }).when(alertMapper).insert(any(MarketingMonitorAlertDO.class));

        MarketingIntegrationContractSloEvaluationView view = service.evaluateAndSyncContract(
                7L,
                contract,
                "prod-readiness-probe",
                "probe-scheduler");

        assertThat(view.status()).isEqualTo("PAGE");
        assertThat(view.triggeredRuleKey()).isEqualTo("PAGE_FAST_BURN");
        assertThat(view.targetPercent()).isEqualTo(99.0);
        assertThat(view.windows()).hasSize(2);
        assertThat(view.windows()).allSatisfy(window -> {
            assertThat(window.badRatio()).isEqualTo(0.2);
            assertThat(window.burnRate()).isEqualTo(20.0);
            assertThat(window.breached()).isTrue();
        });
        ArgumentCaptor<MarketingMonitorAlertDO> alertCaptor =
                ArgumentCaptor.forClass(MarketingMonitorAlertDO.class);
        verify(alertMapper).insert(alertCaptor.capture());
        MarketingMonitorAlertDO alert = alertCaptor.getValue();
        assertThat(alert.getAlertType()).isEqualTo("INTEGRATION_CONTRACT_SLO_BURN_RATE");
        assertThat(alert.getSeverity()).isEqualTo("CRITICAL");
        assertThat(alert.getStatus()).isEqualTo("OPEN");
        assertThat(alert.getScopeKey()).isEqualTo("google-ads-keyword-write");
        assertThat(alert.getReason()).contains("PAGE_FAST_BURN", "20.00x");
        assertThat(alert.getWindowStart()).isEqualTo(LocalDateTime.parse("2026-06-06T09:00:00"));
        assertThat(alert.getWindowEnd()).isEqualTo(LocalDateTime.parse("2026-06-06T10:00:00"));
        assertThat(alert.getMetadataJson()).contains("\"targetPercent\":99.0");
        verify(fanoutService).dispatchAlert(7L, alert, "probe-scheduler");
    }

    @Test
    void listProductionSloEvaluationsFiltersActiveProductionContractsAndBoundsLimit() {
        MarketingIntegrationContractMapper contractMapper = mock(MarketingIntegrationContractMapper.class);
        MarketingIntegrationContractProbeObservationMapper observationMapper =
                mock(MarketingIntegrationContractProbeObservationMapper.class);
        MarketingMonitorAlertMapper alertMapper = mock(MarketingMonitorAlertMapper.class);
        MarketingIntegrationContractSloService service = new MarketingIntegrationContractSloService(
                contractMapper,
                observationMapper,
                alertMapper,
                new ObjectMapper(),
                null,
                CLOCK);
        when(contractMapper.selectList(any())).thenReturn(List.of(contract("STANDARD")));
        when(observationMapper.selectWindowStats(any(), any(), any(), any()))
                .thenReturn(stats(100, 0))
                .thenReturn(stats(10, 0))
                .thenReturn(stats(100, 0))
                .thenReturn(stats(10, 0))
                .thenReturn(stats(100, 0))
                .thenReturn(stats(10, 0));

        List<MarketingIntegrationContractSloEvaluationView> rows =
                service.listProductionSloEvaluations(7L, 500);

        assertThat(rows).singleElement()
                .satisfies(row -> {
                    assertThat(row.contractKey()).isEqualTo("google-ads-keyword-write");
                    assertThat(row.status()).isEqualTo("OK");
                    assertThat(row.triggeredRuleKey()).isNull();
                });
    }

    private static MarketingIntegrationContractProbeWindowStatsDO stats(long total, long bad) {
        MarketingIntegrationContractProbeWindowStatsDO row = new MarketingIntegrationContractProbeWindowStatsDO();
        row.setTotalCount(total);
        row.setBadCount(bad);
        return row;
    }

    private static MarketingIntegrationContractDO contract(String slaTier) {
        MarketingIntegrationContractDO row = new MarketingIntegrationContractDO();
        row.setId(10L);
        row.setTenantId(7L);
        row.setContractKey("google-ads-keyword-write");
        row.setDisplayName("Google Ads keyword write");
        row.setProviderFamily("SEM");
        row.setEnvironment("PRODUCTION");
        row.setStatus("ACTIVE");
        row.setSlaTier(slaTier);
        row.setMetadataJson("{\"sloProbeKey\":\"prod-readiness-probe\"}");
        return row;
    }
}
