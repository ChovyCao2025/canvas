package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimePipelineService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseRealtimePipelineControllerTest {

    @Test
    void listContractsUsesCurrentTenantAndFilter() {
        CdpWarehouseRealtimePipelineService service = mock(CdpWarehouseRealtimePipelineService.class);
        List<CdpWarehouseRealtimePipelineService.PipelineContractView> contracts = List.of(contract("pipeline"));
        when(service.listPipelines(9L, "ACTIVE")).thenReturn(contracts);
        CdpWarehouseRealtimePipelineController controller =
                new CdpWarehouseRealtimePipelineController(service, tenantResolver(9L, "operator"));

        R<List<CdpWarehouseRealtimePipelineService.PipelineContractView>> response =
                controller.listContracts("ACTIVE").block();

        assertThat(response.getData()).isSameAs(contracts);
        verify(service).listPipelines(9L, "ACTIVE");
    }

    @Test
    void upsertContractDelegatesCurrentTenantAndBody() {
        CdpWarehouseRealtimePipelineService service = mock(CdpWarehouseRealtimePipelineService.class);
        CdpWarehouseRealtimePipelineService.PipelineContractView contract = contract("tenant_pipeline");
        when(service.upsertPipeline(eq(9L), any())).thenReturn(contract);
        CdpWarehouseRealtimePipelineController.PipelineContractReq req =
                new CdpWarehouseRealtimePipelineController.PipelineContractReq();
        req.setPipelineKey("tenant_pipeline");
        req.setSourceType("MYSQL_CDC");
        req.setSourceRef("canvas.table");
        req.setProcessorType("FLINK_CDC");
        req.setSinkType("DORIS");
        req.setSinkRef("canvas_ods.table");
        req.setDeliverySemantics("EXACTLY_ONCE");
        CdpWarehouseRealtimePipelineController controller =
                new CdpWarehouseRealtimePipelineController(service, tenantResolver(9L, "operator"));

        R<CdpWarehouseRealtimePipelineService.PipelineContractView> response =
                controller.upsertContract(req).block();

        assertThat(response.getData()).isSameAs(contract);
        verify(service).upsertPipeline(eq(9L), org.mockito.ArgumentMatchers.argThat(command ->
                "tenant_pipeline".equals(command.pipelineKey())
                        && "MYSQL_CDC".equals(command.sourceType())
                        && "canvas_ods.table".equals(command.sinkRef())));
    }

    @Test
    void reportCheckpointUsesExplicitReporter() {
        CdpWarehouseRealtimePipelineService service = mock(CdpWarehouseRealtimePipelineService.class);
        CdpWarehouseRealtimePipelineService.CheckpointReport report = report("pipeline", "PASS");
        when(service.reportCheckpoint(eq(9L), any())).thenReturn(report);
        CdpWarehouseRealtimePipelineController.CheckpointReq req =
                new CdpWarehouseRealtimePipelineController.CheckpointReq();
        req.setPipelineKey("pipeline");
        req.setCheckpointId("chk-1");
        req.setCommittedOffset("100");
        req.setReportedBy("flink-job");
        req.setSourceSchemaVersion("1");
        req.setSinkSchemaVersion("2");
        req.setWatermarkTime("2026-06-06T01:59:00Z");
        req.setCheckpointTime("2026-06-06T02:00:00Z");
        CdpWarehouseRealtimePipelineController controller =
                new CdpWarehouseRealtimePipelineController(service, tenantResolver(9L, "operator"));
        LocalDateTime expectedCheckpointTime = OffsetDateTime.parse("2026-06-06T02:00:00Z")
                .atZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime();

        R<CdpWarehouseRealtimePipelineService.CheckpointReport> response =
                controller.reportCheckpoint(req).block();

        assertThat(response.getData()).isSameAs(report);
        verify(service).reportCheckpoint(eq(9L), org.mockito.ArgumentMatchers.argThat(command ->
                "pipeline".equals(command.pipelineKey())
                        && "chk-1".equals(command.checkpointId())
                        && "100".equals(command.committedOffset())
                        && "flink-job".equals(command.reportedBy())
                        && "1".equals(command.sourceSchemaVersion())
                        && "2".equals(command.sinkSchemaVersion())
                        && expectedCheckpointTime.equals(command.checkpointTime())));
    }

    @Test
    void statusDelegatesCurrentTenantAndLimit() {
        CdpWarehouseRealtimePipelineService service = mock(CdpWarehouseRealtimePipelineService.class);
        CdpWarehouseRealtimePipelineService.PipelineStatusSummary summary =
                new CdpWarehouseRealtimePipelineService.PipelineStatusSummary(9L, 1, 1, 0, 0, List.of());
        when(service.status(9L, 3)).thenReturn(summary);
        CdpWarehouseRealtimePipelineController controller =
                new CdpWarehouseRealtimePipelineController(service, tenantResolver(9L, "operator"));

        R<CdpWarehouseRealtimePipelineService.PipelineStatusSummary> response =
                controller.status(3).block();

        assertThat(response.getData()).isSameAs(summary);
        verify(service).status(9L, 3);
    }

    private CdpWarehouseRealtimePipelineService.PipelineContractView contract(String key) {
        return new CdpWarehouseRealtimePipelineService.PipelineContractView(
                1L, 9L, key, key, "MYSQL_CDC", "canvas.table", "canvas.table",
                "cg", "FLINK_CDC", "DORIS", "canvas_ods.table", "EXACTLY_ONCE",
                60, 300_000L, 300, "ACTIVE", "data-platform", "{}",
                null, null, null, null, null, null, null, null, null);
    }

    private CdpWarehouseRealtimePipelineService.CheckpointReport report(String key, String status) {
        return new CdpWarehouseRealtimePipelineService.CheckpointReport(
                1L, 9L, key, "chk-1", "0", "100", "100",
                LocalDateTime.now(), LocalDateTime.now(), 1000L, 10L, status,
                "Realtime pipeline healthy", "flink", List.of());
    }

    private TenantContextResolver tenantResolver(Long tenantId, String username) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", username)));
        return resolver;
    }
}
