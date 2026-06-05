package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.dataset.BiDatasetFieldResource;
import org.chovy.canvas.domain.bi.dataset.BiDatasetResource;
import org.chovy.canvas.domain.bi.dataset.BiDatasetResourceService;
import org.chovy.canvas.domain.bi.dataset.BiDatasetVersionView;
import org.chovy.canvas.domain.bi.dataset.BiMetricResource;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiDatasetControllerTest {

    @Test
    void saveDraftUsesCurrentTenantUserAndLockToken() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDatasetResourceService service = mock(BiDatasetResourceService.class);
        BiDatasetResource request = dataset("DRAFT");
        when(service.saveDraft(7L, "alice", "TENANT_ADMIN", request, "lock-token-1")).thenReturn(request);
        BiDatasetController controller = new BiDatasetController(resolver, service);

        StepVerifier.create(controller.saveDraft("channel_performance_daily", "lock-token-1", request))
                .assertNext(response -> {
                    assertThat(response.getData().datasetKey()).isEqualTo("channel_performance_daily");
                    assertThat(response.getData().status()).isEqualTo("DRAFT");
                })
                .verifyComplete();

        verify(service).saveDraft(7L, "alice", "TENANT_ADMIN", request, "lock-token-1");
    }

    @Test
    void publishUsesCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDatasetResourceService service = mock(BiDatasetResourceService.class);
        when(service.publish(7L, "alice", "TENANT_ADMIN", "channel_performance_daily")).thenReturn(dataset("PUBLISHED"));
        BiDatasetController controller = new BiDatasetController(resolver, service);

        StepVerifier.create(controller.publish("channel_performance_daily"))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("PUBLISHED"))
                .verifyComplete();

        verify(service).publish(7L, "alice", "TENANT_ADMIN", "channel_performance_daily");
    }

    @Test
    void listReturnsDatasetResources() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDatasetResourceService service = mock(BiDatasetResourceService.class);
        when(service.listResources(7L)).thenReturn(List.of(dataset("PUBLISHED")));
        BiDatasetController controller = new BiDatasetController(resolver, service);

        StepVerifier.create(controller.list())
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.datasetType()).isEqualTo("TABLE")))
                .verifyComplete();
    }

    @Test
    void archiveUsesCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDatasetResourceService service = mock(BiDatasetResourceService.class);
        when(service.archive(7L, "channel_performance_daily")).thenReturn(dataset("ARCHIVED"));
        BiDatasetController controller = new BiDatasetController(resolver, service);

        StepVerifier.create(controller.archive("channel_performance_daily"))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("ARCHIVED"))
                .verifyComplete();

        verify(service).archive(7L, "channel_performance_daily");
    }

    @Test
    void listVersionsUsesCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDatasetResourceService service = mock(BiDatasetResourceService.class);
        BiDatasetVersionView version = new BiDatasetVersionView(
                51L,
                "channel_performance_daily",
                2,
                "PUBLISHED",
                dataset("PUBLISHED"),
                "alice",
                null);
        when(service.listVersions(7L, "channel_performance_daily", 5)).thenReturn(List.of(version));
        BiDatasetController controller = new BiDatasetController(resolver, service);

        StepVerifier.create(controller.listVersions("channel_performance_daily", 5))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.version()).isEqualTo(2)))
                .verifyComplete();

        verify(service).listVersions(7L, "channel_performance_daily", 5);
    }

    @Test
    void restoreVersionUsesCurrentTenantUserAndLockToken() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDatasetResourceService service = mock(BiDatasetResourceService.class);
        when(service.restoreVersion(
                7L, "alice", "TENANT_ADMIN", "channel_performance_daily", 2, "lock-token-1"))
                .thenReturn(dataset("DRAFT"));
        BiDatasetController controller = new BiDatasetController(resolver, service);

        StepVerifier.create(controller.restoreVersion("channel_performance_daily", "lock-token-1", 2))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("DRAFT"))
                .verifyComplete();

        verify(service).restoreVersion(
                7L, "alice", "TENANT_ADMIN", "channel_performance_daily", 2, "lock-token-1");
    }

    private BiDatasetResource dataset(String status) {
        return new BiDatasetResource(
                "channel_performance_daily",
                "Channel Performance Daily",
                "TABLE",
                "canvas_dws.channel_performance_daily",
                "tenant_id",
                Map.of("category", "CHANNEL"),
                List.of(new BiDatasetFieldResource("stat_date", "Date", "stat_date", "DIMENSION", "DATE", "DATE", null, null, null, true, "NORMAL", 10)),
                List.of(new BiMetricResource("send_count", "Send Count", "SUM(send_count)", "SUM", "NUMBER", "次", "#,##0", List.of("stat_date"), "alice", "Daily sends", "ACTIVE")),
                status,
                "PERSISTED");
    }
}
