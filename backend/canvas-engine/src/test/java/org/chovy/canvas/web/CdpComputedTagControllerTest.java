package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.CdpComputedTagDefinitionDO;
import org.chovy.canvas.domain.cdp.CdpLineageService;
import org.chovy.canvas.domain.cdp.ComputedTagService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpComputedTagControllerTest {

    @Test
    void managementEndpointsUseCurrentTenant() {
        ComputedTagService tagService = mock(ComputedTagService.class);
        CdpLineageService lineageService = mock(CdpLineageService.class);
        CdpComputedTagController controller = new CdpComputedTagController(
                tenantResolver(42L, "alice"),
                tagService,
                lineageService);
        CdpComputedTagDefinitionDO row = definition("vip_likely");
        when(tagService.list(42L)).thenReturn(List.of(row));
        when(tagService.create(eq(42L), any(ComputedTagService.DefinitionCommand.class), eq("alice"))).thenReturn(row);
        when(tagService.preview(42L, "vip_likely")).thenReturn(new ComputedTagService.PreviewResult(2, 1, List.of()));
        when(tagService.runNow(42L, "vip_likely", "alice")).thenReturn(new ComputedTagService.RunResult(88L, "SUCCESS", 2, 1, 1, 1, 0, null));
        when(lineageService.findTagLineage(42L, "vip_likely")).thenReturn(List.of());
        when(lineageService.checkTypeChange(42L, "vip_likely", "BOOLEAN", "NUMBER"))
                .thenReturn(new CdpLineageService.ImpactCheck(false, "INCOMPATIBLE_TYPE_CHANGE", List.of()));

        assertThat(controller.list().block().getData()).containsExactly(row);
        assertThat(controller.create(new CdpComputedTagController.ComputedTagRequest(
                "vip_likely", "VIP likely", "BOOLEAN", "RULE",
                "{\"field\":\"paidCount\",\"op\":\">=\",\"value\":2}", "MANUAL", List.of()))
                .block().getData()).isEqualTo(row);
        assertThat(controller.preview("vip_likely").block().getData().matchedCount()).isEqualTo(1);
        assertThat(controller.run("vip_likely").block().getData().runId()).isEqualTo(88L);
        assertThat(controller.lineage("vip_likely").block().getData()).isEmpty();
        assertThat(controller.impactCheck("vip_likely", new CdpComputedTagController.ImpactCheckRequest("BOOLEAN", "NUMBER"))
                .block().getData().allowed()).isFalse();

        controller.activate("vip_likely").block();
        controller.pause("vip_likely").block();
        verify(tagService).activate(42L, "vip_likely");
        verify(tagService).pause(42L, "vip_likely");
    }

    private CdpComputedTagDefinitionDO definition(String tagCode) {
        CdpComputedTagDefinitionDO row = new CdpComputedTagDefinitionDO();
        row.setTagCode(tagCode);
        row.setDisplayName("VIP likely");
        return row;
    }

    private TenantContextResolver tenantResolver(Long tenantId, String username) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", username)));
        return resolver;
    }
}
