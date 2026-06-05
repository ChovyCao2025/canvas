package org.chovy.canvas.domain.warehouse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpWarehouseAssetAvailabilityDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseConsumerAvailabilityContractDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseAssetAvailabilityMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseConsumerAvailabilityContractMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseConsumerAvailabilityServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void recordAssetAvailabilityNormalizesAndUpsertsEvidence() {
        CdpWarehouseAssetAvailabilityMapper assetMapper = mock(CdpWarehouseAssetAvailabilityMapper.class);
        CdpWarehouseConsumerAvailabilityService service = service(
                assetMapper,
                mock(CdpWarehouseConsumerAvailabilityContractMapper.class),
                mock(CdpWarehouseAvailabilityService.class));
        LocalDateTime availableUntil = LocalDateTime.of(2026, 6, 5, 12, 0);

        CdpWarehouseConsumerAvailabilityService.AssetAvailabilityView view =
                service.recordAssetAvailability(9L, new CdpWarehouseConsumerAvailabilityService.AssetAvailabilityCommand(
                        "table",
                        "canvas_dws.user_event_metric_daily",
                        "offline",
                        LocalDateTime.of(2026, 6, 5, 0, 0),
                        LocalDateTime.of(2026, 6, 5, 12, 0),
                        availableUntil,
                        "pass",
                        "aggregate_job",
                        "run-1",
                        "ok",
                        LocalDateTime.of(2026, 6, 5, 12, 1)));

        assertThat(view.tenantId()).isEqualTo(9L);
        assertThat(view.assetType()).isEqualTo("TABLE");
        assertThat(view.availabilityMode()).isEqualTo("OFFLINE");
        assertThat(view.status()).isEqualTo("PASS");
        ArgumentCaptor<CdpWarehouseAssetAvailabilityDO> captor =
                ArgumentCaptor.forClass(CdpWarehouseAssetAvailabilityDO.class);
        verify(assetMapper).upsert(captor.capture());
        assertThat(captor.getValue().getEvidenceSource()).isEqualTo("AGGREGATE_JOB");
        assertThat(captor.getValue().getAvailableUntil()).isEqualTo(availableUntil);
    }

    @Test
    void upsertAndListContractsPersistRequiredAssets() {
        CdpWarehouseConsumerAvailabilityContractMapper contractMapper =
                mock(CdpWarehouseConsumerAvailabilityContractMapper.class);
        CdpWarehouseConsumerAvailabilityService service = service(
                mock(CdpWarehouseAssetAvailabilityMapper.class),
                contractMapper,
                mock(CdpWarehouseAvailabilityService.class));

        CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityContractView view =
                service.upsertContract(9L, contractCommand("BLOCK_ON_WARN", 0));

        assertThat(view.contractKey()).isEqualTo("bi_daily_active_users");
        assertThat(view.requiredMode()).isEqualTo("OFFLINE");
        assertThat(view.requiredAssets())
                .containsExactly(new CdpWarehouseConsumerAvailabilityService.AssetRef(
                        "TABLE", "canvas_dws.user_event_metric_daily"));
        ArgumentCaptor<CdpWarehouseConsumerAvailabilityContractDO> captor =
                ArgumentCaptor.forClass(CdpWarehouseConsumerAvailabilityContractDO.class);
        verify(contractMapper).upsert(captor.capture());
        assertThat(captor.getValue().getRequiredAssetsJson())
                .contains("canvas_dws.user_event_metric_daily");

        when(contractMapper.selectList(any())).thenReturn(List.of(captor.getValue()));
        assertThat(service.listContracts(9L, "BI_METRIC", "ACTIVE"))
                .extracting(CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityContractView::contractKey)
                .containsExactly("bi_daily_active_users");
    }

    @Test
    void evaluateBlocksWhenAssetEvidenceIsMissing() throws Exception {
        CdpWarehouseAssetAvailabilityMapper assetMapper = mock(CdpWarehouseAssetAvailabilityMapper.class);
        CdpWarehouseConsumerAvailabilityContractMapper contractMapper =
                mock(CdpWarehouseConsumerAvailabilityContractMapper.class);
        CdpWarehouseAvailabilityService availabilityService = mock(CdpWarehouseAvailabilityService.class);
        LocalDateTime from = LocalDateTime.of(2026, 6, 5, 10, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 5, 11, 0);
        when(contractMapper.selectList(any())).thenReturn(List.of(contractRow("BLOCK_ON_WARN", 0)));
        when(assetMapper.selectList(any())).thenReturn(List.of());
        when(availabilityService.evaluate(9L, from, to, "OFFLINE")).thenReturn(windowDecision("PASS", from, to));
        CdpWarehouseConsumerAvailabilityService service = service(assetMapper, contractMapper, availabilityService);

        CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation evaluation =
                service.evaluateContract(9L, "bi_daily_active_users", from, to);

        assertThat(evaluation.status()).isEqualTo("FAIL");
        assertThat(evaluation.allowed()).isFalse();
        assertThat(evaluation.assetGates()).singleElement()
                .extracting(CdpWarehouseConsumerAvailabilityService.AssetAvailabilityGate::reason)
                .isEqualTo("asset availability evidence is missing");
        verify(contractMapper).updateEvaluation(any(), any(), any(), any(), any());
    }

    @Test
    void evaluateAllowsWarnWhenPolicyBlocksOnlyOnFail() throws Exception {
        CdpWarehouseAssetAvailabilityMapper assetMapper = mock(CdpWarehouseAssetAvailabilityMapper.class);
        CdpWarehouseConsumerAvailabilityContractMapper contractMapper =
                mock(CdpWarehouseConsumerAvailabilityContractMapper.class);
        CdpWarehouseAvailabilityService availabilityService = mock(CdpWarehouseAvailabilityService.class);
        LocalDateTime from = LocalDateTime.of(2026, 6, 5, 10, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 5, 11, 0);
        when(contractMapper.selectList(any())).thenReturn(List.of(contractRow("BLOCK_ON_FAIL", 60)));
        when(assetMapper.selectList(any())).thenReturn(List.of(assetRow(
                "PASS", LocalDateTime.of(2026, 6, 5, 10, 30))));
        when(availabilityService.evaluate(9L, from, to, "OFFLINE")).thenReturn(windowDecision("PASS", from, to));
        CdpWarehouseConsumerAvailabilityService service = service(assetMapper, contractMapper, availabilityService);

        CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation evaluation =
                service.evaluateContract(9L, "bi_daily_active_users", from, to);

        assertThat(evaluation.status()).isEqualTo("WARN");
        assertThat(evaluation.allowed()).isTrue();
        assertThat(evaluation.assetGates().get(0).lagMinutes()).isEqualTo(30);
    }

    @Test
    void evaluateBlocksWhenWindowLevelAvailabilityFails() throws Exception {
        CdpWarehouseAssetAvailabilityMapper assetMapper = mock(CdpWarehouseAssetAvailabilityMapper.class);
        CdpWarehouseConsumerAvailabilityContractMapper contractMapper =
                mock(CdpWarehouseConsumerAvailabilityContractMapper.class);
        CdpWarehouseAvailabilityService availabilityService = mock(CdpWarehouseAvailabilityService.class);
        LocalDateTime from = LocalDateTime.of(2026, 6, 5, 10, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 5, 11, 0);
        when(contractMapper.selectList(any())).thenReturn(List.of(contractRow("BLOCK_ON_FAIL", 60)));
        when(assetMapper.selectList(any())).thenReturn(List.of(assetRow(
                "PASS", LocalDateTime.of(2026, 6, 5, 12, 0))));
        when(availabilityService.evaluate(9L, from, to, "OFFLINE")).thenReturn(windowDecision("FAIL", from, to));
        CdpWarehouseConsumerAvailabilityService service = service(assetMapper, contractMapper, availabilityService);

        CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation evaluation =
                service.evaluateContract(9L, "bi_daily_active_users", from, to);

        assertThat(evaluation.status()).isEqualTo("FAIL");
        assertThat(evaluation.allowed()).isFalse();
        assertThat(evaluation.windowDecision().status()).isEqualTo("FAIL");
    }

    private CdpWarehouseConsumerAvailabilityService service(
            CdpWarehouseAssetAvailabilityMapper assetMapper,
            CdpWarehouseConsumerAvailabilityContractMapper contractMapper,
            CdpWarehouseAvailabilityService availabilityService) {
        return new CdpWarehouseConsumerAvailabilityService(
                assetMapper,
                contractMapper,
                availabilityService,
                objectMapper);
    }

    private CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityContractCommand contractCommand(
            String gatePolicy,
            int warnToleranceMinutes) {
        return new CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityContractCommand(
                "bi_daily_active_users",
                "BI_METRIC",
                "daily_active_users",
                "cdp_user_metrics",
                "daily_active_users",
                "OFFLINE",
                List.of(new CdpWarehouseConsumerAvailabilityService.AssetRef(
                        "TABLE", "canvas_dws.user_event_metric_daily")),
                gatePolicy,
                warnToleranceMinutes,
                "ACTIVE",
                "data-platform",
                "Daily active user metric contract");
    }

    private CdpWarehouseConsumerAvailabilityContractDO contractRow(String gatePolicy,
                                                                   int warnToleranceMinutes)
            throws JsonProcessingException {
        CdpWarehouseConsumerAvailabilityContractDO row = new CdpWarehouseConsumerAvailabilityContractDO();
        row.setId(1L);
        row.setTenantId(9L);
        row.setContractKey("bi_daily_active_users");
        row.setConsumerType("BI_METRIC");
        row.setConsumerRef("daily_active_users");
        row.setDatasetKey("cdp_user_metrics");
        row.setMetricKey("daily_active_users");
        row.setRequiredMode("OFFLINE");
        row.setRequiredAssetsJson(objectMapper.writeValueAsString(List.of(
                new CdpWarehouseConsumerAvailabilityService.AssetRef(
                        "TABLE", "canvas_dws.user_event_metric_daily"))));
        row.setGatePolicy(gatePolicy);
        row.setWarnToleranceMinutes(warnToleranceMinutes);
        row.setStatus("ACTIVE");
        return row;
    }

    private CdpWarehouseAssetAvailabilityDO assetRow(String status, LocalDateTime availableUntil) {
        CdpWarehouseAssetAvailabilityDO row = new CdpWarehouseAssetAvailabilityDO();
        row.setId(10L);
        row.setTenantId(9L);
        row.setAssetType("TABLE");
        row.setAssetKey("canvas_dws.user_event_metric_daily");
        row.setAvailabilityMode("OFFLINE");
        row.setAvailableUntil(availableUntil);
        row.setStatus(status);
        row.setEvidenceSource("AGGREGATE_JOB");
        row.setEvidenceRef("run-1");
        row.setReason("ok");
        row.setObservedAt(LocalDateTime.of(2026, 6, 5, 12, 0));
        return row;
    }

    private CdpWarehouseAvailabilityService.AvailabilityDecision windowDecision(String status,
                                                                                LocalDateTime from,
                                                                                LocalDateTime to) {
        return new CdpWarehouseAvailabilityService.AvailabilityDecision(
                9L,
                "OFFLINE",
                from,
                to,
                LocalDateTime.of(2026, 6, 5, 11, 1),
                status,
                List.of(new CdpWarehouseAvailabilityService.AvailabilityGate(
                        "offline_aggregate",
                        status,
                        "window " + status,
                        to,
                        0L,
                        1)));
    }
}
