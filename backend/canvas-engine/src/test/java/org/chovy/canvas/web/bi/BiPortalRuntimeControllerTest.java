package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.portal.BiPortalMenuResource;
import org.chovy.canvas.domain.bi.portal.BiPortalResource;
import org.chovy.canvas.domain.bi.portal.BiPortalRuntimeService;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiPortalRuntimeControllerTest {

    @Test
    void listPassesCurrentTenantUserAndRole() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "OPERATOR", "alice")));
        BiPortalRuntimeService service = mock(BiPortalRuntimeService.class);
        BiQueryContext context = new BiQueryContext(7L, "alice", "OPERATOR");
        when(service.listPublished(7L, context)).thenReturn(List.of(portal()));
        BiPortalRuntimeController controller = new BiPortalRuntimeController(resolver, service);

        StepVerifier.create(controller.list())
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.portalKey()).isEqualTo("canvas-ops-portal")))
                .verifyComplete();

        verify(service).listPublished(7L, context);
    }

    @Test
    void getPassesCurrentTenantUserAndRole() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "bob")));
        BiPortalRuntimeService service = mock(BiPortalRuntimeService.class);
        BiQueryContext context = new BiQueryContext(7L, "bob", "TENANT_ADMIN");
        when(service.getPublished(7L, "canvas-ops-portal", context)).thenReturn(portal());
        BiPortalRuntimeController controller = new BiPortalRuntimeController(resolver, service);

        StepVerifier.create(controller.get("canvas-ops-portal"))
                .assertNext(response -> assertThat(response.getData().menus()).hasSize(1))
                .verifyComplete();

        verify(service).getPublished(7L, "canvas-ops-portal", context);
    }

    private BiPortalResource portal() {
        return new BiPortalResource(
                "canvas-ops-portal",
                "Canvas Operations Portal",
                Map.of("theme", "light"),
                List.of(new BiPortalMenuResource(
                        "overview",
                        null,
                        "经营总览",
                        "DASHBOARD",
                        "canvas-effect",
                        21L,
                        null,
                        Map.of(),
                        10)),
                "PUBLISHED",
                "PERSISTED");
    }
}
