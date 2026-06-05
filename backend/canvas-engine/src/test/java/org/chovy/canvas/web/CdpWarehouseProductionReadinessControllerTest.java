package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseProductionReadinessProofService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseProductionReadinessControllerTest {

    @Test
    void productionReadinessUsesCurrentTenantWindowModeAndContracts() {
        CdpWarehouseProductionReadinessProofService service =
                mock(CdpWarehouseProductionReadinessProofService.class);
        LocalDateTime from = LocalDateTime.of(2026, 6, 5, 10, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 5, 11, 0);
        CdpWarehouseProductionReadinessProofService.ProductionReadinessProof proof =
                proof(9L, from, to, "HYBRID");
        when(service.proof(9L, from, to, "HYBRID", List.of("bi_daily_active_users", "audience_12")))
                .thenReturn(proof);
        CdpWarehouseProductionReadinessController controller =
                new CdpWarehouseProductionReadinessController(service, tenantResolver(9L));

        R<CdpWarehouseProductionReadinessProofService.ProductionReadinessProof> response =
                controller.productionReadiness(from, to, "HYBRID",
                        List.of("bi_daily_active_users", "audience_12")).block();

        assertThat(response.getData()).isSameAs(proof);
        verify(service).proof(9L, from, to, "HYBRID",
                List.of("bi_daily_active_users", "audience_12"));
    }

    @Test
    void productionReadinessDefaultsTenantWhenResolverIsAbsent() {
        CdpWarehouseProductionReadinessProofService service =
                mock(CdpWarehouseProductionReadinessProofService.class);
        CdpWarehouseProductionReadinessProofService.ProductionReadinessProof proof =
                proof(0L, null, null, "HYBRID");
        when(service.proof(0L, null, null, "HYBRID", List.of())).thenReturn(proof);
        CdpWarehouseProductionReadinessController controller =
                new CdpWarehouseProductionReadinessController(service);

        R<CdpWarehouseProductionReadinessProofService.ProductionReadinessProof> response =
                controller.productionReadiness(null, null, "HYBRID", null).block();

        assertThat(response.getData().tenantId()).isZero();
        verify(service).proof(0L, null, null, "HYBRID", List.of());
    }

    private CdpWarehouseProductionReadinessProofService.ProductionReadinessProof proof(
            Long tenantId,
            LocalDateTime from,
            LocalDateTime to,
            String mode) {
        return new CdpWarehouseProductionReadinessProofService.ProductionReadinessProof(
                tenantId,
                "PASS",
                LocalDateTime.of(2026, 6, 5, 11, 1),
                from,
                to,
                mode,
                List.of(new CdpWarehouseProductionReadinessProofService.ProofEvidence(
                        "warehouse_readiness", "PASS", "ok")),
                null,
                null,
                List.of(),
                null);
    }

    private TenantContextResolver tenantResolver(Long tenantId) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", "operator")));
        return resolver;
    }
}
