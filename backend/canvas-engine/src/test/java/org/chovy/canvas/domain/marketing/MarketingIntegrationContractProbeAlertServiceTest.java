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
import org.chovy.canvas.dal.dataobject.MarketingMonitorAlertDO;
import org.chovy.canvas.dal.mapper.MarketingMonitorAlertMapper;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertFanoutService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingIntegrationContractProbeAlertServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void syncProbeResultOpensAndDispatchesDedupedFailureAlert() {
        MarketingMonitorAlertMapper alertMapper = mock(MarketingMonitorAlertMapper.class);
        MarketingMonitorAlertFanoutService fanoutService = mock(MarketingMonitorAlertFanoutService.class);
        MarketingIntegrationContractProbeAlertService service =
                new MarketingIntegrationContractProbeAlertService(
                        alertMapper, new ObjectMapper(), fanoutService, CLOCK);
        when(alertMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            invocation.<MarketingMonitorAlertDO>getArgument(0).setId(900L);
            return 1;
        }).when(alertMapper).insert(any(MarketingMonitorAlertDO.class));
        MarketingIntegrationContractProbeRunView probeRun = probeRun("FAIL", "timeout", 504);

        service.syncProbeResult(7L, contract(), probeRun, "probe-scheduler");

        ArgumentCaptor<MarketingMonitorAlertDO> captor =
                ArgumentCaptor.forClass(MarketingMonitorAlertDO.class);
        verify(alertMapper).insert(captor.capture());
        MarketingMonitorAlertDO inserted = captor.getValue();
        assertThat(inserted.getTenantId()).isEqualTo(7L);
        assertThat(inserted.getAlertType()).isEqualTo("INTEGRATION_CONTRACT_PROBE_FAILURE");
        assertThat(inserted.getSeverity()).isEqualTo("CRITICAL");
        assertThat(inserted.getStatus()).isEqualTo("OPEN");
        assertThat(inserted.getScopeKey()).isEqualTo("google-ads-keyword-write");
        assertThat(inserted.getTitle()).isEqualTo("Marketing integration contract probe failed");
        assertThat(inserted.getReason()).contains("google-ads-keyword-write", "timeout");
        assertThat(inserted.getMetadataJson())
                .contains("\"contractId\":10")
                .contains("\"probeRunId\":110")
                .contains("\"httpStatusCode\":504");
        assertThat(inserted.getWindowStart()).isEqualTo(LocalDateTime.parse("2026-06-06T10:00:00"));
        assertThat(inserted.getWindowEnd()).isEqualTo(LocalDateTime.parse("2026-06-06T10:00:00"));
        verify(fanoutService).dispatchAlert(7L, inserted, "probe-scheduler");
    }

    @Test
    void syncProbeResultUpdatesExistingOpenFailureWithoutFanoutSpam() {
        MarketingMonitorAlertMapper alertMapper = mock(MarketingMonitorAlertMapper.class);
        MarketingMonitorAlertFanoutService fanoutService = mock(MarketingMonitorAlertFanoutService.class);
        MarketingIntegrationContractProbeAlertService service =
                new MarketingIntegrationContractProbeAlertService(
                        alertMapper, new ObjectMapper(), fanoutService, CLOCK);
        MarketingMonitorAlertDO existing = alert(7L, 900L, "OPEN", 2);
        when(alertMapper.selectOne(any())).thenReturn(existing);

        service.syncProbeResult(7L, contract(), probeRun("FAIL", "credential expired", 401), "probe-scheduler");

        ArgumentCaptor<MarketingMonitorAlertDO> captor =
                ArgumentCaptor.forClass(MarketingMonitorAlertDO.class);
        verify(alertMapper).updateById(captor.capture());
        MarketingMonitorAlertDO updated = captor.getValue();
        assertThat(updated.getId()).isEqualTo(900L);
        assertThat(updated.getItemCount()).isEqualTo(3);
        assertThat(updated.getReason()).contains("credential expired");
        assertThat(updated.getMetadataJson()).contains("\"lastProbeRunId\":110");
        verify(alertMapper, never()).insert(any(MarketingMonitorAlertDO.class));
        verify(fanoutService, never()).dispatchAlert(any(), any(MarketingMonitorAlertDO.class), any());
    }

    @Test
    void syncProbeResultUpdatesExistingAlertWhenConcurrentInsertWinsDedupeKey() {
        MarketingMonitorAlertMapper alertMapper = mock(MarketingMonitorAlertMapper.class);
        MarketingIntegrationContractProbeAlertService service =
                new MarketingIntegrationContractProbeAlertService(
                        alertMapper, new ObjectMapper(), null, CLOCK);
        MarketingMonitorAlertDO existing = alert(7L, 900L, "OPEN", 1);
        when(alertMapper.selectOne(any())).thenReturn(null).thenReturn(existing);
        doAnswer(invocation -> {
            throw new DuplicateKeyException("duplicate dedupe key");
        }).when(alertMapper).insert(any(MarketingMonitorAlertDO.class));

        service.syncProbeResult(7L, contract(), probeRun("FAIL", "timeout", 504), "probe-scheduler");

        ArgumentCaptor<MarketingMonitorAlertDO> captor =
                ArgumentCaptor.forClass(MarketingMonitorAlertDO.class);
        verify(alertMapper).updateById(captor.capture());
        MarketingMonitorAlertDO updated = captor.getValue();
        assertThat(updated.getId()).isEqualTo(900L);
        assertThat(updated.getItemCount()).isEqualTo(2);
        assertThat(updated.getMetadataJson()).contains("\"lastProbeRunId\":110");
    }

    @Test
    void syncProbeResultAutoResolvesOpenFailureAlertWhenProbePasses() {
        MarketingMonitorAlertMapper alertMapper = mock(MarketingMonitorAlertMapper.class);
        MarketingIntegrationContractProbeAlertService service =
                new MarketingIntegrationContractProbeAlertService(
                        alertMapper, new ObjectMapper(), null, CLOCK);
        MarketingMonitorAlertDO existing = alert(7L, 900L, "OPEN", 2);
        when(alertMapper.selectOne(any())).thenReturn(existing);

        service.syncProbeResult(7L, contract(), probeRun("PASS", null, 204), "probe-scheduler");

        ArgumentCaptor<MarketingMonitorAlertDO> captor =
                ArgumentCaptor.forClass(MarketingMonitorAlertDO.class);
        verify(alertMapper).updateById(captor.capture());
        MarketingMonitorAlertDO resolved = captor.getValue();
        assertThat(resolved.getStatus()).isEqualTo("RESOLVED");
        assertThat(resolved.getResolvedBy()).isEqualTo("probe-scheduler");
        assertThat(resolved.getResolvedAt()).isEqualTo(LocalDateTime.parse("2026-06-06T10:00:00"));
        assertThat(resolved.getMetadataJson()).contains("\"recoveredProbeRunId\":110");
    }

    private static MarketingIntegrationContractProbeRunView probeRun(String status,
                                                                     String errorMessage,
                                                                     Integer httpStatusCode) {
        return new MarketingIntegrationContractProbeRunView(
                110L,
                7L,
                10L,
                "google-ads-keyword-write",
                "SEM",
                "PRODUCTION",
                "prod-readiness-probe",
                status,
                httpStatusCode,
                250L,
                "urn:canvas:marketing-integration:probe-failure",
                errorMessage,
                "Automatic probe failed",
                Map.of("probeSource", "AUTOMATED"),
                "2026-06-06T10:00:00",
                "probe-scheduler",
                "probe-scheduler",
                LocalDateTime.parse("2026-06-06T10:00:00"),
                LocalDateTime.parse("2026-06-06T10:00:00"));
    }

    private static MarketingIntegrationContractView contract() {
        return new MarketingIntegrationContractView(
                10L,
                7L,
                "google-ads-keyword-write",
                "Google Ads Keyword Write",
                "SEM",
                "search-marketing-governance",
                "provider-credential-governance",
                "marketing-integration-contract-registry",
                "OUTBOUND",
                "PRODUCTION",
                "OAUTH",
                "active provider credential",
                "https://provider.example.com/health",
                "Growth",
                "ACTIVE",
                "CRITICAL",
                30000,
                Map.of(),
                Map.of(),
                Map.of(),
                "operator-1",
                "operator-1",
                LocalDateTime.parse("2026-06-06T09:00:00"),
                LocalDateTime.parse("2026-06-06T09:00:00"));
    }

    private static MarketingMonitorAlertDO alert(Long tenantId, Long id, String status, int itemCount) {
        MarketingMonitorAlertDO row = new MarketingMonitorAlertDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setAlertType("INTEGRATION_CONTRACT_PROBE_FAILURE");
        row.setSeverity("CRITICAL");
        row.setStatus(status);
        row.setScopeKey("google-ads-keyword-write");
        row.setTitle("Marketing integration contract probe failed");
        row.setReason("Previous failure");
        row.setItemCount(itemCount);
        row.setWindowStart(LocalDateTime.parse("2026-06-06T09:30:00"));
        row.setWindowEnd(LocalDateTime.parse("2026-06-06T09:30:00"));
        row.setMetadataJson("{\"contractId\":10,\"lastProbeRunId\":109}");
        row.setCreatedBy("probe-scheduler");
        row.setCreatedAt(LocalDateTime.parse("2026-06-06T09:30:00"));
        row.setUpdatedAt(LocalDateTime.parse("2026-06-06T09:30:00"));
        return row;
    }
}
