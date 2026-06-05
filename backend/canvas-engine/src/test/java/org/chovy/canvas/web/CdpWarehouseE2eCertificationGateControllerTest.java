package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseE2eCertificationGateService;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseE2eCertificationGateControllerTest {

    @Test
    void routeUsesNestedE2eCertificationPrefix() {
        RequestMapping mapping = CdpWarehouseE2eCertificationGateController.class.getAnnotation(RequestMapping.class);

        assertThat(mapping.value()).containsExactly("/warehouse/e2e-certification/gate");
    }

    @Test
    void gateRouteUsesGetRoot() throws Exception {
        GetMapping mapping = CdpWarehouseE2eCertificationGateController.class
                .getMethod("gate", String.class, List.class, boolean.class, boolean.class, boolean.class, long.class)
                .getAnnotation(GetMapping.class);

        assertThat(mapping.value()).isEmpty();
    }

    @Test
    void gateUsesCurrentTenantAndRequestParameters() {
        CdpWarehouseE2eCertificationGateService service = mock(CdpWarehouseE2eCertificationGateService.class);
        CdpWarehouseE2eCertificationGateService.GateDecision decision = new CdpWarehouseE2eCertificationGateService.GateDecision(
                9L,
                "PASS",
                "fresh PASS certification evidence",
                101L,
                "PASS",
                LocalDateTime.of(2026, 6, 5, 11, 0),
                LocalDateTime.of(2026, 6, 5, 12, 0),
                "HYBRID",
                true,
                true,
                true,
                60,
                List.of("audience_12"));
        when(service.evaluate(9L, "HYBRID", List.of("audience_12"), true, true, true, 60)).thenReturn(decision);
        CdpWarehouseE2eCertificationGateController controller =
                new CdpWarehouseE2eCertificationGateController(service, tenantResolver(9L));

        R<CdpWarehouseE2eCertificationGateService.GateDecision> response =
                controller.gate("HYBRID", List.of("audience_12"), true, true, true, 60).block();

        assertThat(response.getData()).isSameAs(decision);
        verify(service).evaluate(9L, "HYBRID", List.of("audience_12"), true, true, true, 60);
    }

    private TenantContextResolver tenantResolver(Long tenantId) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", "operator")));
        return resolver;
    }
}
