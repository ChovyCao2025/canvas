package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseSloPolicyService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseSloPolicyControllerTest {

    @Test
    void listPoliciesUsesCurrentTenant() {
        CdpWarehouseSloPolicyService service = mock(CdpWarehouseSloPolicyService.class);
        CdpWarehouseSloPolicyService.SloPolicyView policy = policy(9L);
        when(service.listPolicies(9L, "ACTIVE")).thenReturn(List.of(policy));
        CdpWarehouseSloPolicyController controller =
                new CdpWarehouseSloPolicyController(service, tenantResolver(9L));

        R<List<CdpWarehouseSloPolicyService.SloPolicyView>> response =
                controller.listPolicies("ACTIVE").block();

        assertThat(response.getData()).containsExactly(policy);
        verify(service).listPolicies(9L, "ACTIVE");
    }

    @Test
    void effectiveUsesCurrentTenantAndPolicyKey() {
        CdpWarehouseSloPolicyService service = mock(CdpWarehouseSloPolicyService.class);
        CdpWarehouseSloPolicyService.SloPolicyView policy = policy(9L);
        when(service.effectivePolicy(9L, CdpWarehouseSloPolicyService.DEFAULT_POLICY_KEY)).thenReturn(policy);
        CdpWarehouseSloPolicyController controller =
                new CdpWarehouseSloPolicyController(service, tenantResolver(9L));

        R<CdpWarehouseSloPolicyService.SloPolicyView> response =
                controller.effective(CdpWarehouseSloPolicyService.DEFAULT_POLICY_KEY).block();

        assertThat(response.getData()).isSameAs(policy);
        verify(service).effectivePolicy(9L, CdpWarehouseSloPolicyService.DEFAULT_POLICY_KEY);
    }

    @Test
    void upsertUsesCurrentTenantAndRequestBody() {
        CdpWarehouseSloPolicyService service = mock(CdpWarehouseSloPolicyService.class);
        CdpWarehouseSloPolicyService.SloPolicyView policy = policy(9L);
        CdpWarehouseSloPolicyController.SloPolicyReq req = new CdpWarehouseSloPolicyController.SloPolicyReq();
        req.setPolicyKey("warehouse_readiness_default");
        req.setOfflineWarnRunGapMinutes(10);
        req.setOfflineFailRunGapMinutes(30);
        req.setOfflineWarnWatermarkLagMinutes(10);
        req.setOfflineFailWatermarkLagMinutes(30);
        req.setAudienceWarnRunGapMinutes(60);
        req.setAudienceFailRunGapMinutes(180);
        when(service.upsertPolicy(org.mockito.ArgumentMatchers.eq(9L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(policy);
        CdpWarehouseSloPolicyController controller =
                new CdpWarehouseSloPolicyController(service, tenantResolver(9L));

        R<CdpWarehouseSloPolicyService.SloPolicyView> response = controller.upsert(req).block();

        assertThat(response.getData()).isSameAs(policy);
        verify(service).upsertPolicy(org.mockito.ArgumentMatchers.eq(9L), argThat(command ->
                "warehouse_readiness_default".equals(command.policyKey())
                        && command.offlineWarnRunGapMinutes().equals(10)
                        && command.audienceFailRunGapMinutes().equals(180)));
    }

    private CdpWarehouseSloPolicyService.SloPolicyView policy(Long tenantId) {
        return new CdpWarehouseSloPolicyService.SloPolicyView(
                1L,
                tenantId,
                CdpWarehouseSloPolicyService.DEFAULT_POLICY_KEY,
                "policy",
                10,
                30,
                10,
                30,
                60,
                180,
                "ACTIVE",
                "data-platform",
                "policy");
    }

    private TenantContextResolver tenantResolver(Long tenantId) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", "operator")));
        return resolver;
    }
}
