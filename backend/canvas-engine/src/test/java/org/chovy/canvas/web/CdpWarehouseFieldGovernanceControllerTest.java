package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.MarketingBiDatasetRegistry;
import org.chovy.canvas.domain.warehouse.CdpWarehouseFieldGovernanceService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseFieldGovernanceControllerTest {

    @Test
    void listPoliciesUsesCurrentTenantAndFilters() {
        CdpWarehouseFieldGovernanceService service = mock(CdpWarehouseFieldGovernanceService.class);
        List<CdpWarehouseFieldGovernanceService.FieldPolicyView> policies = List.of(policy("canvas_id"));
        when(service.listPolicies(9L, "canvas_daily_stats", "ACTIVE")).thenReturn(policies);
        CdpWarehouseFieldGovernanceController controller =
                new CdpWarehouseFieldGovernanceController(service, tenantResolver(9L, "TENANT_ADMIN", "alice"));

        R<List<CdpWarehouseFieldGovernanceService.FieldPolicyView>> response =
                controller.listPolicies("canvas_daily_stats", "ACTIVE").block();

        assertThat(response.getData()).isSameAs(policies);
        verify(service).listPolicies(9L, "canvas_daily_stats", "ACTIVE");
    }

    @Test
    void upsertPolicyDelegatesTenantAndRequestBody() {
        CdpWarehouseFieldGovernanceService service = mock(CdpWarehouseFieldGovernanceService.class);
        CdpWarehouseFieldGovernanceService.FieldPolicyView policy = policy("canvas_id");
        when(service.upsertPolicy(eq(9L), any())).thenReturn(policy);
        CdpWarehouseFieldGovernanceController.FieldPolicyReq req =
                new CdpWarehouseFieldGovernanceController.FieldPolicyReq();
        req.setDatasetKey("canvas_daily_stats");
        req.setFieldKey("canvas_id");
        req.setPhysicalName("canvas_dws.canvas_daily_stats");
        req.setColumnName("canvas_id");
        req.setValueType("NUMBER");
        req.setAccessPolicy("MASK");
        req.setMinRole("TENANT_ADMIN");
        CdpWarehouseFieldGovernanceController controller =
                new CdpWarehouseFieldGovernanceController(service, tenantResolver(9L, "TENANT_ADMIN", "alice"));

        R<CdpWarehouseFieldGovernanceService.FieldPolicyView> response =
                controller.upsertPolicy(req).block();

        assertThat(response.getData()).isSameAs(policy);
        verify(service).upsertPolicy(eq(9L), org.mockito.ArgumentMatchers.argThat(command ->
                "canvas_daily_stats".equals(command.datasetKey())
                        && "canvas_id".equals(command.fieldKey())
                        && "MASK".equals(command.accessPolicy())
                        && "TENANT_ADMIN".equals(command.minRole())));
    }

    @Test
    void evaluateBiQueryUsesCurrentTenantRoleAndDatasetResolver() {
        CdpWarehouseFieldGovernanceService service = mock(CdpWarehouseFieldGovernanceService.class);
        BiQueryRequest request = new BiQueryRequest(
                "canvas_daily_stats", List.of("canvas_id"), List.of(), List.of(), List.of(), 100);
        CdpWarehouseFieldGovernanceService.BiPolicyEvaluation evaluation =
                new CdpWarehouseFieldGovernanceService.BiPolicyEvaluation(
                        9L, "canvas_daily_stats", "alice", "OPERATOR",
                        CdpWarehouseFieldGovernanceService.ACTION_BI_EVALUATE, false, List.of(), "denied");
        when(service.evaluateBiQuery(any(), eq(request), any(), eq(CdpWarehouseFieldGovernanceService.ACTION_BI_EVALUATE)))
                .thenReturn(evaluation);
        AtomicReference<Long> tenantSeen = new AtomicReference<>();
        BiDatasetSpecResolver resolver = new BiDatasetSpecResolver() {
            @Override
            public org.chovy.canvas.domain.bi.query.BiDatasetSpec dataset(String datasetKey, Long tenantId) {
                tenantSeen.set(tenantId);
                return MarketingBiDatasetRegistry.dataset(datasetKey);
            }

            @Override
            public List<org.chovy.canvas.domain.bi.query.BiDatasetSpec> datasets(Long tenantId) {
                return List.of(MarketingBiDatasetRegistry.dataset("canvas_daily_stats"));
            }
        };
        CdpWarehouseFieldGovernanceController controller =
                new CdpWarehouseFieldGovernanceController(
                        service, tenantResolver(9L, "OPERATOR", "alice"), resolver);

        R<CdpWarehouseFieldGovernanceService.BiPolicyEvaluation> response =
                controller.evaluateBiQuery(request).block();

        assertThat(response.getData()).isSameAs(evaluation);
        assertThat(tenantSeen.get()).isEqualTo(9L);
        verify(service).evaluateBiQuery(any(), eq(request),
                org.mockito.ArgumentMatchers.argThat((BiQueryContext context) ->
                        context.tenantId().equals(9L)
                                && "alice".equals(context.username())
                                && "OPERATOR".equals(context.role())),
                eq(CdpWarehouseFieldGovernanceService.ACTION_BI_EVALUATE));
    }

    private CdpWarehouseFieldGovernanceService.FieldPolicyView policy(String fieldKey) {
        return new CdpWarehouseFieldGovernanceService.FieldPolicyView(
                1L, 9L, "canvas_daily_stats", fieldKey, "canvas_dws.canvas_daily_stats",
                fieldKey, "NUMBER", "ID", "PII_RELATED", "MASK", "TENANT_ADMIN",
                "SELECT,FILTER", "HASH", "ACTIVE", "data-platform", "policy");
    }

    private TenantContextResolver tenantResolver(Long tenantId, String role, String username) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, role, username)));
        return resolver;
    }
}
