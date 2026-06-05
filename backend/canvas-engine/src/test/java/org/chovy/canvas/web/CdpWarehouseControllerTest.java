package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseAggregationService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseBackfillService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseOperationsService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRetentionService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseControllerTest {

    @Test
    void statusUsesCurrentTenant() {
        CdpWarehouseOperationsService service = mock(CdpWarehouseOperationsService.class);
        TenantContextResolver tenantResolver = tenantResolver(9L);
        CdpWarehouseOperationsService.WarehouseStatus status =
                new CdpWarehouseOperationsService.WarehouseStatus(9L, List.of(), List.of());
        when(service.status(9L, 20)).thenReturn(status);
        CdpWarehouseController controller = new CdpWarehouseController(service, tenantResolver);

        R<CdpWarehouseOperationsService.WarehouseStatus> response = controller.status(20).block();

        assertThat(response.getData()).isSameAs(status);
        verify(service).status(9L, 20);
    }

    @Test
    void backfillUsesCurrentTenantAndRequestBounds() {
        CdpWarehouseOperationsService service = mock(CdpWarehouseOperationsService.class);
        CdpWarehouseBackfillService.BackfillResult result =
                new CdpWarehouseBackfillService.BackfillResult("SUCCESS", 2, 0, 12);
        when(service.triggerBackfill(9L, 10L, 100, "operator")).thenReturn(result);
        CdpWarehouseController.BackfillReq req = new CdpWarehouseController.BackfillReq();
        req.setLastId(10L);
        req.setLimit(100);
        req.setOperator("operator");
        CdpWarehouseController controller = new CdpWarehouseController(service, tenantResolver(9L));

        R<CdpWarehouseBackfillService.BackfillResult> response = controller.backfill(req).block();

        assertThat(response.getData()).isSameAs(result);
        verify(service).triggerBackfill(9L, 10L, 100, "operator");
    }

    @Test
    void aggregateUsesCurrentTenantAndRequestWindow() {
        CdpWarehouseOperationsService service = mock(CdpWarehouseOperationsService.class);
        LocalDateTime from = LocalDateTime.of(2026, 6, 5, 10, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 5, 11, 0);
        CdpWarehouseAggregationService.AggregationResult result =
                new CdpWarehouseAggregationService.AggregationResult("SUCCESS", 3, 2);
        when(service.triggerAggregation(9L, from, to, "operator")).thenReturn(result);
        CdpWarehouseController.AggregateReq req = new CdpWarehouseController.AggregateReq();
        req.setFrom(from);
        req.setTo(to);
        req.setOperator("operator");
        CdpWarehouseController controller = new CdpWarehouseController(service, tenantResolver(9L));

        R<CdpWarehouseAggregationService.AggregationResult> response = controller.aggregate(req).block();

        assertThat(response.getData()).isSameAs(result);
        verify(service).triggerAggregation(9L, from, to, "operator");
    }

    @Test
    void offlineCyclePlanUsesCurrentTenantAndParameters() {
        CdpWarehouseOperationsService service = mock(CdpWarehouseOperationsService.class);
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);
        CdpWarehouseOperationsService.OfflineCyclePlan plan =
                new CdpWarehouseOperationsService.OfflineCyclePlan(9L, now, 100, 30, List.of());
        when(service.planOfflineCycle(9L, now, 100, 30)).thenReturn(plan);
        CdpWarehouseController controller = new CdpWarehouseController(service, tenantResolver(9L));

        R<CdpWarehouseOperationsService.OfflineCyclePlan> response =
                controller.offlineCyclePlan(100, 30, now).block();

        assertThat(response.getData()).isSameAs(plan);
        verify(service).planOfflineCycle(9L, now, 100, 30);
    }

    @Test
    void offlineCycleRunUsesCurrentTenantAndRequest() {
        CdpWarehouseOperationsService service = mock(CdpWarehouseOperationsService.class);
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);
        CdpWarehouseOperationsService.OfflineCycleResult result =
                new CdpWarehouseOperationsService.OfflineCycleResult(
                        100L, 9L, "SUCCESS", 3, 0, null, List.of());
        when(service.runOfflineCycle(9L, now, 100, 30, "operator")).thenReturn(result);
        CdpWarehouseController.OfflineCycleReq req = new CdpWarehouseController.OfflineCycleReq();
        req.setNow(now);
        req.setBackfillLimit(100);
        req.setAggregationWindowMinutes(30);
        req.setOperator("operator");
        CdpWarehouseController controller = new CdpWarehouseController(service, tenantResolver(9L));

        R<CdpWarehouseOperationsService.OfflineCycleResult> response =
                controller.offlineCycleRun(req).block();

        assertThat(response.getData()).isSameAs(result);
        verify(service).runOfflineCycle(9L, now, 100, 30, "operator");
    }

    @Test
    void retentionPlanUsesCurrentTenantAndParameters() {
        CdpWarehouseOperationsService operations = mock(CdpWarehouseOperationsService.class);
        CdpWarehouseRetentionService retention = mock(CdpWarehouseRetentionService.class);
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);
        CdpWarehouseRetentionService.RetentionTargetPlan empty =
                new CdpWarehouseRetentionService.RetentionTargetPlan("SYNC_RUNS", 30, now, 0, "none");
        CdpWarehouseRetentionService.RetentionPlan plan =
                new CdpWarehouseRetentionService.RetentionPlan(9L, now, empty, empty, empty, 0);
        when(retention.plan(9L, now, 30, 14, 90)).thenReturn(plan);
        CdpWarehouseController controller = new CdpWarehouseController(
                operations, retention, tenantResolver(9L));

        R<CdpWarehouseRetentionService.RetentionPlan> response =
                controller.retentionPlan(30, 14, 90, now).block();

        assertThat(response.getData()).isSameAs(plan);
        verify(retention).plan(9L, now, 30, 14, 90);
    }

    @Test
    void retentionRunUsesCurrentTenantAndRequest() {
        CdpWarehouseOperationsService operations = mock(CdpWarehouseOperationsService.class);
        CdpWarehouseRetentionService retention = mock(CdpWarehouseRetentionService.class);
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);
        CdpWarehouseRetentionService.RetentionTargetResult empty =
                new CdpWarehouseRetentionService.RetentionTargetResult("SYNC_RUNS", 30, now, 0, 0);
        CdpWarehouseRetentionService.RetentionCleanupResult result =
                new CdpWarehouseRetentionService.RetentionCleanupResult(
                        9L, now, "operator", empty, empty, empty, 0);
        when(retention.cleanup(9L, now, 30, 14, 90, "operator")).thenReturn(result);
        CdpWarehouseController.RetentionReq req = new CdpWarehouseController.RetentionReq();
        req.setNow(now);
        req.setSyncRunRetentionDays(30);
        req.setRealtimeRetryRetentionDays(14);
        req.setResolvedIncidentRetentionDays(90);
        req.setOperator("operator");
        CdpWarehouseController controller = new CdpWarehouseController(
                operations, retention, tenantResolver(9L));

        R<CdpWarehouseRetentionService.RetentionCleanupResult> response =
                controller.retentionRun(req).block();

        assertThat(response.getData()).isSameAs(result);
        verify(retention).cleanup(9L, now, 30, 14, 90, "operator");
    }

    private TenantContextResolver tenantResolver(Long tenantId) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", "operator")));
        return resolver;
    }
}
