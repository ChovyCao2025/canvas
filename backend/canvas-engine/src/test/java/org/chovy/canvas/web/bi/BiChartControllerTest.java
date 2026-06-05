package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.chart.BiChartResource;
import org.chovy.canvas.domain.bi.chart.BiChartResourceService;
import org.chovy.canvas.domain.bi.chart.BiChartVersionView;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiChartControllerTest {

    @Test
    void saveDraftUsesCurrentTenantUserAndLockToken() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiChartResourceService service = mock(BiChartResourceService.class);
        BiChartResource request = chart("DRAFT");
        BiChartResource persisted = chart("DRAFT");
        when(service.saveDraft(7L, "alice", "TENANT_ADMIN", request, "lock-token-1")).thenReturn(persisted);
        BiChartController controller = new BiChartController(resolver, service);

        StepVerifier.create(controller.saveDraft("trend-executions", "lock-token-1", request))
                .assertNext(response -> {
                    assertThat(response.getData().chartKey()).isEqualTo("trend-executions");
                    assertThat(response.getData().status()).isEqualTo("DRAFT");
                })
                .verifyComplete();

        verify(service).saveDraft(7L, "alice", "TENANT_ADMIN", request, "lock-token-1");
    }

    @Test
    void publishUsesCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiChartResourceService service = mock(BiChartResourceService.class);
        when(service.publish(7L, "alice", "TENANT_ADMIN", "trend-executions"))
                .thenReturn(chart("PUBLISHED"));
        BiChartController controller = new BiChartController(resolver, service);

        StepVerifier.create(controller.publish("trend-executions"))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("PUBLISHED"))
                .verifyComplete();

        verify(service).publish(7L, "alice", "TENANT_ADMIN", "trend-executions");
    }

    @Test
    void listReturnsChartResources() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiChartResourceService service = mock(BiChartResourceService.class);
        when(service.list(7L)).thenReturn(List.of(chart("PUBLISHED")));
        BiChartController controller = new BiChartController(resolver, service);

        StepVerifier.create(controller.list())
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.chartType()).isEqualTo("LINE")))
                .verifyComplete();
    }

    @Test
    void archiveUsesCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiChartResourceService service = mock(BiChartResourceService.class);
        when(service.archive(7L, "trend-executions")).thenReturn(chart("ARCHIVED"));
        BiChartController controller = new BiChartController(resolver, service);

        StepVerifier.create(controller.archive("trend-executions"))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("ARCHIVED"))
                .verifyComplete();

        verify(service).archive(7L, "trend-executions");
    }

    @Test
    void listVersionsUsesCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiChartResourceService service = mock(BiChartResourceService.class);
        var version = new BiChartVersionView(
                88L,
                "trend-executions",
                2,
                "PUBLISHED",
                chart("PUBLISHED"),
                "alice",
                java.time.LocalDateTime.parse("2026-06-05T10:00:00"));
        when(service.listVersions(7L, "trend-executions", 5)).thenReturn(List.of(version));
        BiChartController controller = new BiChartController(resolver, service);

        StepVerifier.create(controller.listVersions("trend-executions", 5))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> {
                            assertThat(item.version()).isEqualTo(2);
                            assertThat(item.resource().chartType()).isEqualTo("LINE");
                        }))
                .verifyComplete();

        verify(service).listVersions(7L, "trend-executions", 5);
    }

    @Test
    void restoreVersionUsesCurrentTenantUserAndLockToken() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiChartResourceService service = mock(BiChartResourceService.class);
        when(service.restoreVersion(7L, "alice", "TENANT_ADMIN", "trend-executions", 2, "lock-token-1"))
                .thenReturn(chart("DRAFT"));
        BiChartController controller = new BiChartController(resolver, service);

        StepVerifier.create(controller.restoreVersion("trend-executions", "lock-token-1", 2))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("DRAFT"))
                .verifyComplete();

        verify(service).restoreVersion(7L, "alice", "TENANT_ADMIN", "trend-executions", 2, "lock-token-1");
    }

    private BiChartResource chart(String status) {
        BiQueryRequest query = new BiQueryRequest(
                "canvas_daily_stats",
                List.of("stat_date"),
                List.of("total_executions"),
                List.of(),
                List.of(),
                500);
        return new BiChartResource(
                "trend-executions",
                "Execution Trend",
                "LINE",
                "canvas_daily_stats",
                query,
                Map.of("palette", "workbench"),
                Map.of("click", "FILTER_LINKAGE"),
                status,
                "PERSISTED");
    }
}
