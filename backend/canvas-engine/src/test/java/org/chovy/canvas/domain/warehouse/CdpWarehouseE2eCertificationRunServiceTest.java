package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.CdpWarehouseE2eCertificationRunDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseE2eCertificationRunMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseE2eCertificationRunServiceTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 6, 5, 10, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 6, 5, 11, 0);

    @Test
    void runPersistsSuccessfulCertificationEvidence() {
        CdpWarehousePhysicalE2eCertificationService certificationService =
                mock(CdpWarehousePhysicalE2eCertificationService.class);
        CdpWarehouseE2eCertificationRunMapper mapper = mock(CdpWarehouseE2eCertificationRunMapper.class);
        CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification certification =
                certification("PASS");
        when(certificationService.certify(9L, FROM, TO, "HYBRID",
                List.of("bi_daily_active_users", "audience_12"), true, false, false))
                .thenReturn(certification);
        CdpWarehouseE2eCertificationRunService service =
                new CdpWarehouseE2eCertificationRunService(certificationService, mapper);

        CdpWarehouseE2eCertificationRunService.CertificationRunView view =
                service.run(9L, FROM, TO, "HYBRID",
                        List.of("bi_daily_active_users", "audience_12"), true, "operator");

        assertThat(view.status()).isEqualTo("PASS");
        assertThat(view.tenantId()).isEqualTo(9L);
        assertThat(view.requestedBy()).isEqualTo("operator");
        ArgumentCaptor<CdpWarehouseE2eCertificationRunDO> captor =
                ArgumentCaptor.forClass(CdpWarehouseE2eCertificationRunDO.class);
        verify(mapper).insert(captor.capture());
        CdpWarehouseE2eCertificationRunDO row = captor.getValue();
        assertThat(row.getTenantId()).isEqualTo(9L);
        assertThat(row.getStatus()).isEqualTo("PASS");
        assertThat(row.getMode()).isEqualTo("HYBRID");
        assertThat(row.getRequirePhysical()).isEqualTo(1);
        assertThat(row.getRequireRealtime()).isEqualTo(0);
        assertThat(row.getRequireDataPathProof()).isEqualTo(0);
        assertThat(row.getContractKeysJson()).contains("bi_daily_active_users", "audience_12");
        assertThat(row.getEvidenceJson()).contains("doris_jdbc_connectivity");
        assertThat(row.getProductionReadinessJson()).contains("PASS");
        assertThat(row.getLiveTableInspectionJson()).contains("cdp_user_event_fact");
        assertThat(row.getErrorMessage()).isNull();
    }

    @Test
    void runPersistsRealtimeRequirementAndRealtimeSummaries() {
        CdpWarehousePhysicalE2eCertificationService certificationService =
                mock(CdpWarehousePhysicalE2eCertificationService.class);
        CdpWarehouseE2eCertificationRunMapper mapper = mock(CdpWarehouseE2eCertificationRunMapper.class);
        CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification certification =
                certification("PASS", true);
        when(certificationService.certify(9L, FROM, TO, "HYBRID", List.of("audience_12"), true, true, false))
                .thenReturn(certification);
        CdpWarehouseE2eCertificationRunService service =
                new CdpWarehouseE2eCertificationRunService(certificationService, mapper);

        CdpWarehouseE2eCertificationRunService.CertificationRunView view =
                service.run(9L, FROM, TO, "HYBRID", List.of("audience_12"), true, true, "operator");

        assertThat(view.requireRealtime()).isTrue();
        assertThat(view.realtimePipelineStatusJson()).contains("\"total\":1");
        assertThat(view.realtimeJobStatusJson()).contains("\"total\":1");
        ArgumentCaptor<CdpWarehouseE2eCertificationRunDO> captor =
                ArgumentCaptor.forClass(CdpWarehouseE2eCertificationRunDO.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getRequireRealtime()).isEqualTo(1);
        assertThat(captor.getValue().getRealtimePipelineStatusJson()).contains("\"passed\":1");
        assertThat(captor.getValue().getRealtimeJobStatusJson()).contains("\"passed\":1");
    }

    @Test
    void runPersistsDataPathProofRequirementAndSummary() {
        CdpWarehousePhysicalE2eCertificationService certificationService =
                mock(CdpWarehousePhysicalE2eCertificationService.class);
        CdpWarehouseE2eCertificationRunMapper mapper = mock(CdpWarehouseE2eCertificationRunMapper.class);
        CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification certification =
                certification("PASS", true, true);
        when(certificationService.certify(9L, FROM, TO, "HYBRID", List.of("audience_12"), true, true, true))
                .thenReturn(certification);
        CdpWarehouseE2eCertificationRunService service =
                new CdpWarehouseE2eCertificationRunService(certificationService, mapper);

        CdpWarehouseE2eCertificationRunService.CertificationRunView view =
                service.run(9L, FROM, TO, "HYBRID", List.of("audience_12"), true, true, true, "operator");

        assertThat(view.requireDataPathProof()).isTrue();
        assertThat(view.dataPathProofJson()).contains("\"status\":\"PASS\"", "\"odsRowCount\":1");
        ArgumentCaptor<CdpWarehouseE2eCertificationRunDO> captor =
                ArgumentCaptor.forClass(CdpWarehouseE2eCertificationRunDO.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getRequireDataPathProof()).isEqualTo(1);
        assertThat(captor.getValue().getDataPathProofJson()).contains("\"sinkStatus\":\"PASS\"");
    }

    @Test
    void runPersistsFailWhenCertificationThrows() {
        CdpWarehousePhysicalE2eCertificationService certificationService =
                mock(CdpWarehousePhysicalE2eCertificationService.class);
        CdpWarehouseE2eCertificationRunMapper mapper = mock(CdpWarehouseE2eCertificationRunMapper.class);
        when(certificationService.certify(9L, FROM, TO, "HYBRID", List.of("audience_12"), true, false, false))
                .thenThrow(new IllegalStateException("Doris is unavailable"));
        CdpWarehouseE2eCertificationRunService service =
                new CdpWarehouseE2eCertificationRunService(certificationService, mapper);

        CdpWarehouseE2eCertificationRunService.CertificationRunView view =
                service.run(9L, FROM, TO, "HYBRID", List.of("audience_12"), true, "operator");

        assertThat(view.status()).isEqualTo("FAIL");
        assertThat(view.errorMessage()).contains("Doris is unavailable");
        ArgumentCaptor<CdpWarehouseE2eCertificationRunDO> captor =
                ArgumentCaptor.forClass(CdpWarehouseE2eCertificationRunDO.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAIL");
        assertThat(captor.getValue().getErrorMessage()).contains("Doris is unavailable");
    }

    @Test
    void recentReturnsTenantScopedRows() {
        CdpWarehouseE2eCertificationRunMapper mapper = mock(CdpWarehouseE2eCertificationRunMapper.class);
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(row(101L, 9L, "PASS")));
        CdpWarehouseE2eCertificationRunService service =
                new CdpWarehouseE2eCertificationRunService(
                        mock(CdpWarehousePhysicalE2eCertificationService.class), mapper);

        List<CdpWarehouseE2eCertificationRunService.CertificationRunView> runs =
                service.recent(9L, 20);

        assertThat(runs).singleElement().satisfies(run -> {
            assertThat(run.id()).isEqualTo(101L);
            assertThat(run.tenantId()).isEqualTo(9L);
            assertThat(run.status()).isEqualTo("PASS");
        });
    }

    @Test
    void getRejectsRowsOutsideTenantScope() {
        CdpWarehouseE2eCertificationRunMapper mapper = mock(CdpWarehouseE2eCertificationRunMapper.class);
        when(mapper.selectById(101L)).thenReturn(row(101L, 8L, "PASS"));
        CdpWarehouseE2eCertificationRunService service =
                new CdpWarehouseE2eCertificationRunService(
                        mock(CdpWarehousePhysicalE2eCertificationService.class), mapper);

        assertThatThrownBy(() -> service.get(9L, 101L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    private CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification certification(String status) {
        return certification(status, false);
    }

    private CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification certification(
            String status,
            boolean requireRealtime) {
        return certification(status, requireRealtime, false);
    }

    private CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification certification(
            String status,
            boolean requireRealtime,
            boolean requireDataPathProof) {
        return new CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification(
                9L,
                status,
                TO.plusMinutes(1),
                FROM,
                TO,
                "HYBRID",
                true,
                requireRealtime,
                requireDataPathProof,
                List.of(new CdpWarehousePhysicalE2eCertificationService.CertificationEvidence(
                        "doris_jdbc_connectivity", status, "ok")),
                new CdpWarehouseProductionReadinessProofService.ProductionReadinessProof(
                        9L,
                        status,
                        TO.plusMinutes(1),
                        FROM,
                        TO,
                        "HYBRID",
                        List.of(),
                        null,
                        null,
                        List.of(),
                        null),
                new CdpWarehouseTableGovernanceService.InspectionSummary(
                        9L,
                        1,
                        1,
                        0,
                        0,
                        List.of(new CdpWarehouseTableGovernanceService.InspectionReport(
                                1L,
                                9L,
                                "cdp_user_event_fact",
                                "canvas_dwd.cdp_user_event_fact",
                                status,
                                8,
                                0,
                                List.of(),
                                status,
                                "LIVE:SHOW_CREATE_TABLE",
                                TO))),
                requireRealtime
                        ? new CdpWarehouseRealtimePipelineService.PipelineStatusSummary(9L, 1, 1, 0, 0, List.of())
                        : null,
                requireRealtime
                        ? new CdpWarehouseRealtimeJobControlService.JobStatusSummary(9L, 1, 1, 0, 0, List.of())
                        : null,
                requireDataPathProof
                        ? new CdpWarehouseSyntheticDataPathProbeService.ProbeRunView(
                                1L,
                                9L,
                                "e2e-certification",
                                "warehouse-probe-1",
                                "__warehouse_probe__",
                                "__warehouse_probe_user_1",
                                true,
                                "PASS",
                                "PASS",
                                "PASS",
                                1L,
                                FROM,
                                TO,
                                null,
                                "[]",
                                FROM,
                                TO)
                        : null);
    }

    private CdpWarehouseE2eCertificationRunDO row(Long id, Long tenantId, String status) {
        CdpWarehouseE2eCertificationRunDO row = new CdpWarehouseE2eCertificationRunDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setStatus(status);
        row.setMode("HYBRID");
        row.setRequirePhysical(1);
        row.setRequireRealtime(1);
        row.setRequireDataPathProof(1);
        row.setWindowStart(FROM);
        row.setWindowEnd(TO);
        row.setContractKeysJson("[\"audience_12\"]");
        row.setEvidenceJson("[{\"key\":\"doris_jdbc_connectivity\"}]");
        row.setRequestedBy("operator");
        row.setStartedAt(FROM);
        row.setFinishedAt(TO);
        return row;
    }
}
