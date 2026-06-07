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
