package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseMetricChangeReviewService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseMetricChangeReviewControllerTest {

    @Test
    void listUsesCurrentTenantAndFilters() {
        CdpWarehouseMetricChangeReviewService service = mock(CdpWarehouseMetricChangeReviewService.class);
        CdpWarehouseMetricChangeReviewService.MetricChangeReviewView view = view(9L);
        when(service.list(9L, "canvas_daily_stats", "success_rate", "PENDING_REVIEW"))
                .thenReturn(List.of(view));
        CdpWarehouseMetricChangeReviewController controller =
                new CdpWarehouseMetricChangeReviewController(service, tenantResolver());

        R<List<CdpWarehouseMetricChangeReviewService.MetricChangeReviewView>> response =
                controller.list("canvas_daily_stats", "success_rate", "PENDING_REVIEW").block();

        assertThat(response.getData()).containsExactly(view);
        verify(service).list(9L, "canvas_daily_stats", "success_rate", "PENDING_REVIEW");
    }

    @Test
    void requestChangeUsesCurrentTenantAndUsername() {
        CdpWarehouseMetricChangeReviewService service = mock(CdpWarehouseMetricChangeReviewService.class);
        CdpWarehouseMetricChangeReviewService.MetricChangeReviewView view = view(9L);
        when(service.requestChange(eq(9L), eq("operator-1"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(view);
        CdpWarehouseMetricChangeReviewController controller =
                new CdpWarehouseMetricChangeReviewController(service, tenantResolver());
        CdpWarehouseMetricChangeReviewController.MetricChangeReq req =
                new CdpWarehouseMetricChangeReviewController.MetricChangeReq();
        req.setDatasetKey("canvas_daily_stats");
        req.setMetricKey("success_rate");
        req.setProposedExpression("SUM(success_count)");
        req.setProposedAllowedDimensions(List.of("stat_date"));
        req.setReason("align metric");

        R<CdpWarehouseMetricChangeReviewService.MetricChangeReviewView> response =
                controller.requestChange(req).block();

        assertThat(response.getData()).isSameAs(view);
        verify(service).requestChange(eq(9L), eq("operator-1"), argThat(command ->
                "canvas_daily_stats".equals(command.datasetKey())
                        && "success_rate".equals(command.metricKey())
                        && command.proposedAllowedDimensions().contains("stat_date")));
    }

    @Test
    void decisionAndApplyUseCurrentTenant() {
        CdpWarehouseMetricChangeReviewService service = mock(CdpWarehouseMetricChangeReviewService.class);
        CdpWarehouseMetricChangeReviewService.MetricChangeReviewView view = view(9L);
        when(service.approve(9L, 55L, "operator-1", "approved")).thenReturn(view);
        when(service.reject(9L, 55L, "operator-1", "rejected")).thenReturn(view);
        when(service.apply(9L, 55L)).thenReturn(view);
        CdpWarehouseMetricChangeReviewController controller =
                new CdpWarehouseMetricChangeReviewController(service, tenantResolver());

        CdpWarehouseMetricChangeReviewController.ReviewDecisionReq approveReq =
                new CdpWarehouseMetricChangeReviewController.ReviewDecisionReq();
        approveReq.setReviewNote("approved");
        CdpWarehouseMetricChangeReviewController.ReviewDecisionReq rejectReq =
                new CdpWarehouseMetricChangeReviewController.ReviewDecisionReq();
        rejectReq.setReviewNote("rejected");

        assertThat(controller.approve(55L, approveReq).block().getData()).isSameAs(view);
        assertThat(controller.reject(55L, rejectReq).block().getData()).isSameAs(view);
        assertThat(controller.apply(55L).block().getData()).isSameAs(view);
        verify(service).approve(9L, 55L, "operator-1", "approved");
        verify(service).reject(9L, 55L, "operator-1", "rejected");
        verify(service).apply(9L, 55L);
    }

    private CdpWarehouseMetricChangeReviewService.MetricChangeReviewView view(Long tenantId) {
        return new CdpWarehouseMetricChangeReviewService.MetricChangeReviewView(
                55L,
                tenantId,
                "canvas_daily_stats",
                "success_rate",
                new CdpWarehouseMetricChangeReviewService.MetricSnapshot(
                        "success_rate", "SUM(success_count)", "PERCENT", List.of("stat_date")),
                new CdpWarehouseMetricChangeReviewService.MetricSnapshot(
                        "success_rate", "SUM(success_count)", "PERCENT", List.of("stat_date")),
                new CdpWarehouseMetricChangeReviewService.ImpactSummary(1, 0, 0, 0, 0, List.of()),
                "MEDIUM",
                CdpWarehouseMetricChangeReviewService.STATUS_PENDING_REVIEW,
                "operator-1",
                "reason",
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private TenantContextResolver tenantResolver() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(9L, "TENANT_ADMIN", "operator-1")));
        return resolver;
    }
}
