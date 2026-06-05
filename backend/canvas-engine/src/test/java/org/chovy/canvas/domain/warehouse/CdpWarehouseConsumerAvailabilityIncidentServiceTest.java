package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseConsumerAvailabilityIncidentServiceTest {

    @Test
    void scanSingleWarnContractOpensConsumerAvailabilityIncident() {
        CdpWarehouseConsumerAvailabilityService consumerService = mock(CdpWarehouseConsumerAvailabilityService.class);
        CdpWarehouseIncidentService incidentService = mock(CdpWarehouseIncidentService.class);
        CdpWarehouseConsumerAvailabilityIncidentService service =
                new CdpWarehouseConsumerAvailabilityIncidentService(consumerService, incidentService);
        LocalDateTime from = LocalDateTime.parse("2026-06-05T11:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-06-05T12:00:00");
        when(consumerService.evaluateContract(9L, "bi_revenue", from, to))
                .thenReturn(evaluation("bi_revenue", "BI_METRIC", "revenue", "WARN", false));

        CdpWarehouseConsumerAvailabilityIncidentService.ScanResult result =
                service.scan(9L, "bi_revenue", null, from, to, "qa");

        assertThat(result.tenantId()).isEqualTo(9L);
        assertThat(result.contractKey()).isEqualTo("bi_revenue");
        assertThat(result.worstStatus()).isEqualTo("WARN");
        assertThat(result.totalContracts()).isEqualTo(1);
        assertThat(result.opened()).isEqualTo(1);
        assertThat(result.resolved()).isZero();
        assertThat(result.skipped()).isZero();
        assertThat(result.failed()).isZero();
        verify(incidentService).recordConsumerAvailabilityIncident(argThat(input ->
                input.tenantId().equals(9L)
                        && "bi_revenue".equals(input.contractKey())
                        && "BI_METRIC".equals(input.consumerType())
                        && "WARN".equals(input.contractStatus())
                        && input.assetGateSummaries().get(0).contains("TABLE:canvas_dws.user_event_metric_daily")));
    }

    @Test
    void scanActiveContractsResolvesPassOpensFailAndCountsEvaluationFailure() {
        CdpWarehouseConsumerAvailabilityService consumerService = mock(CdpWarehouseConsumerAvailabilityService.class);
        CdpWarehouseIncidentService incidentService = mock(CdpWarehouseIncidentService.class);
        CdpWarehouseConsumerAvailabilityIncidentService service =
                new CdpWarehouseConsumerAvailabilityIncidentService(consumerService, incidentService);
        LocalDateTime from = LocalDateTime.parse("2026-06-05T11:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-06-05T12:00:00");
        when(consumerService.listContracts(9L, "BI_METRIC", "ACTIVE"))
                .thenReturn(List.of(contract("bi_revenue"), contract("bi_orders"), contract("bi_broken")));
        when(consumerService.evaluateContract(9L, "bi_revenue", from, to))
                .thenReturn(evaluation("bi_revenue", "BI_METRIC", "revenue", "PASS", true));
        when(consumerService.evaluateContract(9L, "bi_orders", from, to))
                .thenReturn(evaluation("bi_orders", "BI_METRIC", "orders", "FAIL", false));
        when(consumerService.evaluateContract(9L, "bi_broken", from, to))
                .thenThrow(new IllegalStateException("contract not readable"));
        when(incidentService.resolveConsumerAvailabilityIncident(9L, "bi_revenue", "qa"))
                .thenReturn(true);

        CdpWarehouseConsumerAvailabilityIncidentService.ScanResult result =
                service.scan(9L, null, "bi_metric", from, to, "qa");

        assertThat(result.consumerType()).isEqualTo("BI_METRIC");
        assertThat(result.worstStatus()).isEqualTo("FAIL");
        assertThat(result.totalContracts()).isEqualTo(3);
        assertThat(result.opened()).isEqualTo(1);
        assertThat(result.resolved()).isEqualTo(1);
        assertThat(result.skipped()).isZero();
        assertThat(result.failed()).isEqualTo(1);
        verify(incidentService).resolveConsumerAvailabilityIncident(9L, "bi_revenue", "qa");
        verify(incidentService).recordConsumerAvailabilityIncident(argThat(input ->
                "bi_orders".equals(input.contractKey()) && "FAIL".equals(input.contractStatus())));
    }

    @Test
    void scanCountsIncidentWriteFailuresWithoutAbortingRemainingContracts() {
        CdpWarehouseConsumerAvailabilityService consumerService = mock(CdpWarehouseConsumerAvailabilityService.class);
        CdpWarehouseIncidentService incidentService = mock(CdpWarehouseIncidentService.class);
        CdpWarehouseConsumerAvailabilityIncidentService service =
                new CdpWarehouseConsumerAvailabilityIncidentService(consumerService, incidentService);
        LocalDateTime from = LocalDateTime.parse("2026-06-05T11:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-06-05T12:00:00");
        when(consumerService.listContracts(9L, null, "ACTIVE"))
                .thenReturn(List.of(contract("audience_daily"), contract("bi_revenue")));
        when(consumerService.evaluateContract(9L, "audience_daily", from, to))
                .thenReturn(evaluation("audience_daily", "AUDIENCE", "audience_12", "WARN", false));
        when(consumerService.evaluateContract(9L, "bi_revenue", from, to))
                .thenReturn(evaluation("bi_revenue", "BI_METRIC", "revenue", "PASS", true));
        doThrow(new IllegalStateException("incident store unavailable"))
                .when(incidentService)
                .recordConsumerAvailabilityIncident(argThat(input -> "audience_daily".equals(input.contractKey())));

        CdpWarehouseConsumerAvailabilityIncidentService.ScanResult result =
                service.scan(9L, null, null, from, to, "");

        assertThat(result.totalContracts()).isEqualTo(2);
        assertThat(result.opened()).isZero();
        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        verify(incidentService).resolveConsumerAvailabilityIncident(
                9L, "bi_revenue", "consumer-availability-incident");
    }

    private CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityContractView contract(String key) {
        return new CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityContractView(
                1L,
                9L,
                key,
                "BI_METRIC",
                key,
                "canvas_daily_stats",
                "success_rate",
                "HYBRID",
                List.of(new CdpWarehouseConsumerAvailabilityService.AssetRef(
                        "TABLE", "canvas_dws.user_event_metric_daily")),
                "BLOCK_ON_WARN",
                0,
                "ACTIVE",
                "ops",
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation evaluation(
            String contractKey,
            String consumerType,
            String consumerRef,
            String status,
            boolean allowed) {
        LocalDateTime from = LocalDateTime.parse("2026-06-05T11:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-06-05T12:00:00");
        return new CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation(
                9L,
                contractKey,
                consumerType,
                consumerRef,
                "HYBRID",
                from,
                to,
                to.plusMinutes(1),
                status,
                allowed,
                "BLOCK_ON_WARN",
                null,
                List.of(new CdpWarehouseConsumerAvailabilityService.AssetAvailabilityGate(
                        "TABLE",
                        "canvas_dws.user_event_metric_daily",
                        status,
                        "test " + status,
                        to.minusMinutes(5),
                        5L,
                        "AGGREGATE_JOB",
                        "run:42",
                        to)),
                "consumer availability " + status);
    }
}
