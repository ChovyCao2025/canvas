package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.CdpComputedProfileAttributeDO;
import org.chovy.canvas.dal.dataobject.CdpComputedProfileRunDO;
import org.chovy.canvas.dal.dataobject.CdpProfileAttributeChangeLogDO;
import org.chovy.canvas.domain.cdp.ComputedProfileAttributeService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpComputedProfileControllerTest {

    @Test
    void createPreviewActivatePauseRunAndAuditUseCurrentTenant() {
        ComputedProfileAttributeService service = mock(ComputedProfileAttributeService.class);
        CdpComputedProfileController controller = new CdpComputedProfileController(
                tenantResolver(42L, "alice"),
                service);
        CdpComputedProfileAttributeDO created = attribute(1L, "lifecycle_stage");
        CdpComputedProfileRunDO run = new CdpComputedProfileRunDO();
        run.setId(99L);
        CdpProfileAttributeChangeLogDO change = new CdpProfileAttributeChangeLogDO();
        change.setOldValue("Lead");
        change.setNewValue("VIP");
        when(service.list(42L)).thenReturn(List.of(created));
        when(service.create(eq(42L), any(CdpComputedProfileAttributeDO.class), eq("alice"))).thenReturn(created);
        when(service.preview(42L, 1L)).thenReturn(new ComputedProfileAttributeService.PreviewResult(
                10, 6, 4, 2, List.of()));
        when(service.runNow(42L, 1L, "alice")).thenReturn(new ComputedProfileAttributeService.RunResult(
                99L, CdpComputedProfileRunDO.SUCCESS, 10, 6, 4, 2));
        when(service.listRuns(42L, 1L, 100)).thenReturn(List.of(run));
        when(service.listChangeLogs(42L, 1L, null, 100)).thenReturn(List.of(change));

        assertThat(controller.list().block().getData()).containsExactly(created);
        assertThat(controller.create(attribute(null, "lifecycle_stage")).block().getData()).isEqualTo(created);
        assertThat(controller.preview(1L).block().getData().unchangedCount()).isEqualTo(2);
        assertThat(controller.run(1L).block().getData().runId()).isEqualTo(99L);
        assertThat(controller.runs(1L, null).block().getData()).containsExactly(run);
        assertThat(controller.changes(1L, null, null).block().getData()).containsExactly(change);

        controller.activate(1L).block();
        controller.pause(1L).block();
        verify(service).activate(42L, 1L);
        verify(service).pause(42L, 1L);
    }

    private CdpComputedProfileAttributeDO attribute(Long id, String attrCode) {
        CdpComputedProfileAttributeDO row = new CdpComputedProfileAttributeDO();
        row.setId(id);
        row.setAttrCode(attrCode);
        row.setDisplayName(attrCode);
        row.setValueType("STRING");
        row.setComputeType("RULE");
        row.setExpressionJson("{\"field\":\"paidCount\",\"op\":\">=\",\"value\":2,\"then\":\"VIP\"}");
        row.setRefreshMode("MANUAL");
        return row;
    }

    private TenantContextResolver tenantResolver(Long tenantId, String username) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", username)));
        return resolver;
    }
}
