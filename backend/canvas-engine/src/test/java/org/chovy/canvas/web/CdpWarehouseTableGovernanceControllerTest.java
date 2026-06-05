package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseTableGovernanceService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseTableGovernanceControllerTest {

    @Test
    void listContractsUsesCurrentTenantAndFilters() {
        CdpWarehouseTableGovernanceService service = mock(CdpWarehouseTableGovernanceService.class);
        List<CdpWarehouseTableGovernanceService.TableContractView> contracts = List.of(contract("canvas_daily_stats"));
        when(service.listContracts(9L, "DWS", "ACTIVE")).thenReturn(contracts);
        CdpWarehouseTableGovernanceController controller =
                new CdpWarehouseTableGovernanceController(service, tenantResolver(9L, "operator"));

        R<List<CdpWarehouseTableGovernanceService.TableContractView>> response =
                controller.listContracts("DWS", "ACTIVE").block();

        assertThat(response.getData()).isSameAs(contracts);
        verify(service).listContracts(9L, "DWS", "ACTIVE");
    }

    @Test
    void upsertContractDelegatesCurrentTenantAndRequestBody() {
        CdpWarehouseTableGovernanceService service = mock(CdpWarehouseTableGovernanceService.class);
        CdpWarehouseTableGovernanceService.TableContractView contract = contract("tenant_metric");
        when(service.upsertContract(eq(9L), any())).thenReturn(contract);
        CdpWarehouseTableGovernanceController.TableContractReq req =
                new CdpWarehouseTableGovernanceController.TableContractReq();
        req.setTableKey("tenant_metric");
        req.setDatasetKey("tenant_metric");
        req.setLayer("DWS");
        req.setPhysicalName("canvas_dws.tenant_metric");
        req.setPartitionColumn("stat_date");
        req.setRetentionDays(365);
        req.setReplicaCount(3);
        req.setBucketCount(16);
        req.setDistributionColumns("tenant_id");
        CdpWarehouseTableGovernanceController controller =
                new CdpWarehouseTableGovernanceController(service, tenantResolver(9L, "operator"));

        R<CdpWarehouseTableGovernanceService.TableContractView> response =
                controller.upsertContract(req).block();

        assertThat(response.getData()).isSameAs(contract);
        verify(service).upsertContract(eq(9L), org.mockito.ArgumentMatchers.argThat(command ->
                "tenant_metric".equals(command.tableKey())
                        && "DWS".equals(command.layer())
                        && "canvas_dws.tenant_metric".equals(command.physicalName())
                        && command.bucketCount() == 16));
    }

    @Test
    void inspectContractDelegatesTenantTableKeyAndExplicitOperator() {
        CdpWarehouseTableGovernanceService service = mock(CdpWarehouseTableGovernanceService.class);
        CdpWarehouseTableGovernanceService.InspectionReport report = report("canvas_daily_stats", "PASS");
        when(service.inspectContract(9L, "canvas_daily_stats", "qa")).thenReturn(report);
        CdpWarehouseTableGovernanceController.InspectionReq req =
                new CdpWarehouseTableGovernanceController.InspectionReq();
        req.setOperator("qa");
        CdpWarehouseTableGovernanceController controller =
                new CdpWarehouseTableGovernanceController(service, tenantResolver(9L, "operator"));

        R<CdpWarehouseTableGovernanceService.InspectionReport> response =
                controller.inspectContract("canvas_daily_stats", req).block();

        assertThat(response.getData()).isSameAs(report);
        verify(service).inspectContract(9L, "canvas_daily_stats", "qa");
    }

    @Test
    void inspectAllFallsBackToCurrentUsername() {
        CdpWarehouseTableGovernanceService service = mock(CdpWarehouseTableGovernanceService.class);
        CdpWarehouseTableGovernanceService.InspectionSummary summary =
                new CdpWarehouseTableGovernanceService.InspectionSummary(
                        9L, 1, 1, 0, 0, List.of(report("canvas_daily_stats", "PASS")));
        when(service.inspectAll(9L, "operator")).thenReturn(summary);
        CdpWarehouseTableGovernanceController controller =
                new CdpWarehouseTableGovernanceController(service, tenantResolver(9L, "operator"));

        R<CdpWarehouseTableGovernanceService.InspectionSummary> response =
                controller.inspectAll(null).block();

        assertThat(response.getData()).isSameAs(summary);
        verify(service).inspectAll(9L, "operator");
    }

    @Test
    void inspectLiveContractDelegatesTenantTableKeyAndExplicitOperator() {
        CdpWarehouseTableGovernanceService service = mock(CdpWarehouseTableGovernanceService.class);
        CdpWarehouseTableGovernanceService.InspectionReport report = report("canvas_daily_stats", "PASS");
        when(service.inspectLiveContract(9L, "canvas_daily_stats", "qa")).thenReturn(report);
        CdpWarehouseTableGovernanceController.InspectionReq req =
                new CdpWarehouseTableGovernanceController.InspectionReq();
        req.setOperator("qa");
        CdpWarehouseTableGovernanceController controller =
                new CdpWarehouseTableGovernanceController(service, tenantResolver(9L, "operator"));

        R<CdpWarehouseTableGovernanceService.InspectionReport> response =
                controller.inspectLiveContract("canvas_daily_stats", req).block();

        assertThat(response.getData()).isSameAs(report);
        verify(service).inspectLiveContract(9L, "canvas_daily_stats", "qa");
    }

    @Test
    void inspectLiveAllFallsBackToCurrentUsername() {
        CdpWarehouseTableGovernanceService service = mock(CdpWarehouseTableGovernanceService.class);
        CdpWarehouseTableGovernanceService.InspectionSummary summary =
                new CdpWarehouseTableGovernanceService.InspectionSummary(
                        9L, 1, 1, 0, 0, List.of(report("canvas_daily_stats", "PASS")));
        when(service.inspectLiveAll(9L, "operator")).thenReturn(summary);
        CdpWarehouseTableGovernanceController controller =
                new CdpWarehouseTableGovernanceController(service, tenantResolver(9L, "operator"));

        R<CdpWarehouseTableGovernanceService.InspectionSummary> response =
                controller.inspectLiveAll(null).block();

        assertThat(response.getData()).isSameAs(summary);
        verify(service).inspectLiveAll(9L, "operator");
    }

    @Test
    void remediationPlanDelegatesTenantTableKeyLiveFlagAndExplicitOperator() {
        CdpWarehouseTableGovernanceService service = mock(CdpWarehouseTableGovernanceService.class);
        CdpWarehouseTableGovernanceService.TableRemediationPlan plan =
                remediationPlan("canvas_daily_stats");
        when(service.planRemediation(9L, "canvas_daily_stats", false, "qa")).thenReturn(plan);
        CdpWarehouseTableGovernanceController.InspectionReq req =
                new CdpWarehouseTableGovernanceController.InspectionReq();
        req.setOperator("qa");
        CdpWarehouseTableGovernanceController controller =
                new CdpWarehouseTableGovernanceController(service, tenantResolver(9L, "operator"));

        R<CdpWarehouseTableGovernanceService.TableRemediationPlan> response =
                controller.remediationPlan("canvas_daily_stats", false, req).block();

        assertThat(response.getData()).isSameAs(plan);
        verify(service).planRemediation(9L, "canvas_daily_stats", false, "qa");
    }

    @Test
    void remediationPlanAllFallsBackToCurrentUsername() {
        CdpWarehouseTableGovernanceService service = mock(CdpWarehouseTableGovernanceService.class);
        CdpWarehouseTableGovernanceService.RemediationSummary summary =
                new CdpWarehouseTableGovernanceService.RemediationSummary(
                        9L, true, 1, 1, 1, 0, List.of(remediationPlan("canvas_daily_stats")));
        when(service.planAllRemediation(9L, true, "operator")).thenReturn(summary);
        CdpWarehouseTableGovernanceController controller =
                new CdpWarehouseTableGovernanceController(service, tenantResolver(9L, "operator"));

        R<CdpWarehouseTableGovernanceService.RemediationSummary> response =
                controller.remediationPlanAll(true, null).block();

        assertThat(response.getData()).isSameAs(summary);
        verify(service).planAllRemediation(9L, true, "operator");
    }

    private CdpWarehouseTableGovernanceService.TableContractView contract(String tableKey) {
        return new CdpWarehouseTableGovernanceService.TableContractView(
                1L, 9L, tableKey, tableKey, "DWS", "canvas_dws." + tableKey,
                "DORIS", "infrastructure/doris/trace-ddl.sql", "stat_date", "DAY",
                730, 3, 8, "canvas_id", null, "ACTIVE", "data-platform",
                "contract", "{}", null, null, null);
    }

    private CdpWarehouseTableGovernanceService.InspectionReport report(String tableKey, String status) {
        return new CdpWarehouseTableGovernanceService.InspectionReport(
                1L, 9L, tableKey, "canvas_dws." + tableKey, status, 8, 0, List.of(),
                "Physical table contract passed", "infrastructure/doris/trace-ddl.sql",
                LocalDateTime.now());
    }

    private CdpWarehouseTableGovernanceService.TableRemediationPlan remediationPlan(String tableKey) {
        return new CdpWarehouseTableGovernanceService.TableRemediationPlan(
                9L,
                true,
                tableKey,
                "canvas_dws." + tableKey,
                "WARN",
                1L,
                "LIVE:SHOW_CREATE_TABLE",
                LocalDateTime.now(),
                1,
                List.of("DDL replication_num is not 3"),
                List.of(new CdpWarehouseTableGovernanceService.RemediationStep(
                        "REPLICATION_NUM",
                        "MEDIUM",
                        true,
                        "DDL replication_num is not 3",
                        "Review and align Doris replica count.",
                        "ALTER TABLE `canvas_dws`.`" + tableKey + "` SET (\"replication_num\" = \"3\");")));
    }

    private TenantContextResolver tenantResolver(Long tenantId, String username) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", username)));
        return resolver;
    }
}
