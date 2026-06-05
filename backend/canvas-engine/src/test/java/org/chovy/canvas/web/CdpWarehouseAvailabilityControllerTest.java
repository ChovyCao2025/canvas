package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseAvailabilityService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseConsumerAvailabilityService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseAvailabilityControllerTest {

    @Test
    void availabilityDelegatesCurrentTenantWindowAndMode() {
        CdpWarehouseAvailabilityService service = mock(CdpWarehouseAvailabilityService.class);
        LocalDateTime from = LocalDateTime.of(2026, 6, 5, 10, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 5, 11, 0);
        CdpWarehouseAvailabilityService.AvailabilityDecision decision =
                new CdpWarehouseAvailabilityService.AvailabilityDecision(
                        9L,
                        "HYBRID",
                        from,
                        to,
                        LocalDateTime.of(2026, 6, 5, 11, 1),
                        "PASS",
                        List.of());
        when(service.evaluate(9L, from, to, "HYBRID")).thenReturn(decision);
        CdpWarehouseAvailabilityController controller =
                new CdpWarehouseAvailabilityController(service, tenantResolver(9L));

        R<CdpWarehouseAvailabilityService.AvailabilityDecision> response =
                controller.availability(from, to, "HYBRID").block();

        assertThat(response.getData()).isSameAs(decision);
        verify(service).evaluate(9L, from, to, "HYBRID");
    }

    @Test
    void recordAssetAvailabilityDelegatesCurrentTenant() {
        CdpWarehouseAvailabilityService availabilityService = mock(CdpWarehouseAvailabilityService.class);
        CdpWarehouseConsumerAvailabilityService consumerService =
                mock(CdpWarehouseConsumerAvailabilityService.class);
        CdpWarehouseConsumerAvailabilityService.AssetAvailabilityCommand command =
                new CdpWarehouseConsumerAvailabilityService.AssetAvailabilityCommand(
                        "TABLE",
                        "canvas_dws.user_event_metric_daily",
                        "OFFLINE",
                        null,
                        null,
                        LocalDateTime.of(2026, 6, 5, 12, 0),
                        "PASS",
                        "AGGREGATE_JOB",
                        "run-1",
                        "ok",
                        LocalDateTime.of(2026, 6, 5, 12, 1));
        CdpWarehouseConsumerAvailabilityService.AssetAvailabilityView view =
                new CdpWarehouseConsumerAvailabilityService.AssetAvailabilityView(
                        1L,
                        9L,
                        "TABLE",
                        "canvas_dws.user_event_metric_daily",
                        "OFFLINE",
                        null,
                        null,
                        LocalDateTime.of(2026, 6, 5, 12, 0),
                        "PASS",
                        "AGGREGATE_JOB",
                        "run-1",
                        "ok",
                        LocalDateTime.of(2026, 6, 5, 12, 1),
                        null,
                        null);
        when(consumerService.recordAssetAvailability(9L, command)).thenReturn(view);
        CdpWarehouseAvailabilityController controller =
                new CdpWarehouseAvailabilityController(availabilityService, tenantResolver(9L), consumerService);

        R<CdpWarehouseConsumerAvailabilityService.AssetAvailabilityView> response =
                controller.recordAssetAvailability(command).block();

        assertThat(response.getData()).isSameAs(view);
        verify(consumerService).recordAssetAvailability(9L, command);
    }

    @Test
    void upsertAndEvaluateContractDelegateCurrentTenant() {
        CdpWarehouseAvailabilityService availabilityService = mock(CdpWarehouseAvailabilityService.class);
        CdpWarehouseConsumerAvailabilityService consumerService =
                mock(CdpWarehouseConsumerAvailabilityService.class);
        CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityContractCommand command =
                new CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityContractCommand(
                        "bi_daily_active_users",
                        "BI_METRIC",
                        "daily_active_users",
                        "cdp_user_metrics",
                        "daily_active_users",
                        "OFFLINE",
                        List.of(new CdpWarehouseConsumerAvailabilityService.AssetRef(
                                "TABLE", "canvas_dws.user_event_metric_daily")),
                        "BLOCK_ON_WARN",
                        0,
                        "ACTIVE",
                        "data-platform",
                        "contract");
        CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityContractView view =
                new CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityContractView(
                        1L,
                        9L,
                        "bi_daily_active_users",
                        "BI_METRIC",
                        "daily_active_users",
                        "cdp_user_metrics",
                        "daily_active_users",
                        "OFFLINE",
                        command.requiredAssets(),
                        "BLOCK_ON_WARN",
                        0,
                        "ACTIVE",
                        "data-platform",
                        "contract",
                        null,
                        null,
                        null,
                        null,
                        null);
        LocalDateTime from = LocalDateTime.of(2026, 6, 5, 10, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 5, 11, 0);
        CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation evaluation =
                new CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation(
                        9L,
                        "bi_daily_active_users",
                        "BI_METRIC",
                        "daily_active_users",
                        "OFFLINE",
                        from,
                        to,
                        LocalDateTime.of(2026, 6, 5, 11, 1),
                        "PASS",
                        true,
                        "BLOCK_ON_WARN",
                        new CdpWarehouseAvailabilityService.AvailabilityDecision(
                                9L,
                                "OFFLINE",
                                from,
                                to,
                                LocalDateTime.of(2026, 6, 5, 11, 1),
                                "PASS",
                                List.of()),
                        List.of(),
                        "ok");
        when(consumerService.upsertContract(9L, command)).thenReturn(view);
        when(consumerService.evaluateContract(9L, "bi_daily_active_users", from, to)).thenReturn(evaluation);
        CdpWarehouseAvailabilityController controller =
                new CdpWarehouseAvailabilityController(availabilityService, tenantResolver(9L), consumerService);

        assertThat(controller.upsertContract(command).block().getData()).isSameAs(view);
        assertThat(controller.evaluateContract("bi_daily_active_users", from, to).block().getData())
                .isSameAs(evaluation);
        verify(consumerService).upsertContract(9L, command);
        verify(consumerService).evaluateContract(9L, "bi_daily_active_users", from, to);
    }

    private TenantContextResolver tenantResolver(Long tenantId) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", "operator")));
        return resolver;
    }
}
