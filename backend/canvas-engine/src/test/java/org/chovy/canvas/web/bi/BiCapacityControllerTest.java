package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacityAlertPolicyCommand;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacityAlertPolicyView;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacityCategoryUsageView;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineConcurrencyQueueView;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacityService;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacitySummaryView;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacityUsageDetailView;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacityUserUsageView;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueJobView;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueService;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueSnapshotView;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineTenantPoolPolicyCommand;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineTenantPoolPolicyView;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiCapacityControllerTest {

    @Test
    void returnsQuickEngineCapacityForCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiQuickEngineCapacityService service = mock(BiQuickEngineCapacityService.class);
        BiQuickEngineCapacitySummaryView summary = new BiQuickEngineCapacitySummaryView(
                7L,
                200_000L,
                173_000L,
                86.5,
                "WARNING",
                true,
                new BiQuickEngineCapacityAlertPolicyView(
                        true,
                        200_000L,
                        80,
                        95,
                        List.of("EMAIL"),
                        List.of("bi-ops"),
                        "ops",
                        null),
                new BiQuickEngineTenantPoolPolicyView(
                        "STANDARD",
                        8,
                        50,
                        120,
                        100,
                        "ops",
                        null),
                new BiQuickEngineConcurrencyQueueView(
                        1,
                        2,
                        0,
                        3,
                        1,
                        12.5,
                        4.0,
                        "NORMAL"),
                List.of(new BiQuickEngineCapacityCategoryUsageView("DATASET_ACCELERATION", 173_000L, 2)),
                List.of(new BiQuickEngineCapacityUsageDetailView(
                        "DATASET_ACCELERATION",
                        "node_daily_stats",
                        90_000L,
                        1,
                        88L,
                        LocalDateTime.of(2026, 6, 6, 10, 0, 0),
                        90_000L,
                        "bob")),
                List.of(new BiQuickEngineCapacityUserUsageView("bob", 90_000L, 1, 1)));
        when(service.summary(7L, 20)).thenReturn(summary);
        BiCapacityController controller = new BiCapacityController(resolver, service);

        StepVerifier.create(controller.quickEngineCapacity(20))
                .assertNext(response -> {
                    assertThat(response.getData().usedRows()).isEqualTo(173_000L);
                    assertThat(response.getData().alertLevel()).isEqualTo("WARNING");
                    assertThat(response.getData().tenantPoolPolicy().maxConcurrentQueries()).isEqualTo(8);
                    assertThat(response.getData().concurrencyQueue().queuedQueries()).isEqualTo(2);
                    assertThat(response.getData().details()).singleElement()
                            .satisfies(item -> assertThat(item.resourceKey()).isEqualTo("node_daily_stats"));
                })
                .verifyComplete();

        verify(service).summary(7L, 20);
    }

    @Test
    void upsertsQuickEngineCapacityAlertPolicyForCurrentTenantAndUser() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiQuickEngineCapacityService service = mock(BiQuickEngineCapacityService.class);
        BiQuickEngineCapacityAlertPolicyCommand command = new BiQuickEngineCapacityAlertPolicyCommand(
                true,
                500_000L,
                75,
                95,
                List.of("LARK", "EMAIL"),
                List.of("bi-ops", "alice"));
        BiQuickEngineCapacityAlertPolicyView policy = new BiQuickEngineCapacityAlertPolicyView(
                true,
                500_000L,
                75,
                95,
                List.of("LARK", "EMAIL"),
                List.of("bi-ops", "alice"),
                "alice",
                LocalDateTime.of(2026, 6, 6, 12, 0, 0));
        when(service.upsertAlertPolicy(7L, command, "alice")).thenReturn(policy);
        BiCapacityController controller = new BiCapacityController(resolver, service);

        StepVerifier.create(controller.upsertQuickEngineCapacityAlertPolicy(command))
                .assertNext(response -> {
                    assertThat(response.getData().enabled()).isTrue();
                    assertThat(response.getData().capacityLimitRows()).isEqualTo(500_000L);
                    assertThat(response.getData().notificationChannels()).containsExactly("LARK", "EMAIL");
                })
                .verifyComplete();

        verify(service).upsertAlertPolicy(7L, command, "alice");
    }

    @Test
    void upsertsQuickEngineTenantPoolPolicyForCurrentTenantAndUser() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiQuickEngineCapacityService service = mock(BiQuickEngineCapacityService.class);
        BiQuickEngineTenantPoolPolicyCommand command = new BiQuickEngineTenantPoolPolicyCommand(
                "gold",
                16,
                120,
                300,
                200);
        BiQuickEngineTenantPoolPolicyView policy = new BiQuickEngineTenantPoolPolicyView(
                "GOLD",
                16,
                120,
                300,
                200,
                "alice",
                LocalDateTime.of(2026, 6, 6, 12, 0, 0));
        when(service.upsertTenantPoolPolicy(7L, command, "alice")).thenReturn(policy);
        BiCapacityController controller = new BiCapacityController(resolver, service);

        StepVerifier.create(controller.upsertQuickEngineTenantPoolPolicy(command))
                .assertNext(response -> {
                    assertThat(response.getData().poolKey()).isEqualTo("GOLD");
                    assertThat(response.getData().maxConcurrentQueries()).isEqualTo(16);
                    assertThat(response.getData().queueLimit()).isEqualTo(120);
                    assertThat(response.getData().queueTimeoutSeconds()).isEqualTo(300);
                    assertThat(response.getData().poolWeight()).isEqualTo(200);
                })
                .verifyComplete();

        verify(service).upsertTenantPoolPolicy(7L, command, "alice");
    }

    @Test
    void returnsQuickEngineQueueSnapshotForCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiQuickEngineCapacityService capacityService = mock(BiQuickEngineCapacityService.class);
        BiQuickEngineQueueService queueService = mock(BiQuickEngineQueueService.class);
        BiQuickEngineQueueSnapshotView snapshot = new BiQuickEngineQueueSnapshotView(
                7L,
                "GOLD",
                3L,
                1L,
                8L,
                2L,
                14L,
                List.of(new BiQuickEngineQueueJobView(
                        81L,
                        7L,
                        "GOLD",
                        "hash-queue",
                        "canvas_daily_stats",
                        "alice",
                        "QUEUED",
                        0,
                        LocalDateTime.of(2026, 6, 6, 12, 0, 0),
                        LocalDateTime.of(2026, 6, 6, 12, 2, 0),
                        null,
                        null,
                        null,
                        null,
                        LocalDateTime.of(2026, 6, 6, 12, 0, 0),
                        LocalDateTime.of(2026, 6, 6, 12, 0, 0))));
        when(queueService.snapshot(7L, "gold", "queued", 20)).thenReturn(snapshot);
        BiCapacityController controller = new BiCapacityController(resolver, capacityService, queueService);

        StepVerifier.create(controller.quickEngineQueue("gold", "queued", 20))
                .assertNext(response -> {
                    assertThat(response.getData().poolKey()).isEqualTo("GOLD");
                    assertThat(response.getData().queued()).isEqualTo(3L);
                    assertThat(response.getData().jobs()).singleElement()
                            .satisfies(job -> assertThat(job.sqlHash()).isEqualTo("hash-queue"));
                })
                .verifyComplete();

        verify(queueService).snapshot(7L, "gold", "queued", 20);
    }
}
