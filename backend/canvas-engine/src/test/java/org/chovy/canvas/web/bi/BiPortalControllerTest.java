package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.portal.BiPortalMenuResource;
import org.chovy.canvas.domain.bi.portal.BiPortalResource;
import org.chovy.canvas.domain.bi.portal.BiPortalResourceService;
import org.chovy.canvas.domain.bi.portal.BiPortalVersionView;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiPortalControllerTest {

    @Test
    void saveDraftUsesCurrentTenantUserAndLockToken() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiPortalResourceService service = mock(BiPortalResourceService.class);
        BiPortalResource request = portal("DRAFT");
        when(service.saveDraft(7L, "alice", "TENANT_ADMIN", request, "lock-token-1")).thenReturn(request);
        BiPortalController controller = new BiPortalController(resolver, service);

        StepVerifier.create(controller.saveDraft("canvas-ops-portal", "lock-token-1", request))
                .assertNext(response -> {
                    assertThat(response.getData().portalKey()).isEqualTo("canvas-ops-portal");
                    assertThat(response.getData().status()).isEqualTo("DRAFT");
                })
                .verifyComplete();

        verify(service).saveDraft(7L, "alice", "TENANT_ADMIN", request, "lock-token-1");
    }

    @Test
    void listReturnsPortalResources() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiPortalResourceService service = mock(BiPortalResourceService.class);
        when(service.list(7L)).thenReturn(List.of(portal("PUBLISHED")));
        BiPortalController controller = new BiPortalController(resolver, service);

        StepVerifier.create(controller.list())
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.name()).isEqualTo("Canvas Operations Portal")))
                .verifyComplete();
    }

    @Test
    void publishUsesCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiPortalResourceService service = mock(BiPortalResourceService.class);
        when(service.publish(7L, "alice", "TENANT_ADMIN", "canvas-ops-portal"))
                .thenReturn(portal("PUBLISHED"));
        BiPortalController controller = new BiPortalController(resolver, service);

        StepVerifier.create(controller.publish("canvas-ops-portal"))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("PUBLISHED"))
                .verifyComplete();

        verify(service).publish(7L, "alice", "TENANT_ADMIN", "canvas-ops-portal");
    }

    @Test
    void archiveUsesCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiPortalResourceService service = mock(BiPortalResourceService.class);
        when(service.archive(7L, "canvas-ops-portal")).thenReturn(portal("ARCHIVED"));
        BiPortalController controller = new BiPortalController(resolver, service);

        StepVerifier.create(controller.archive("canvas-ops-portal"))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("ARCHIVED"))
                .verifyComplete();

        verify(service).archive(7L, "canvas-ops-portal");
    }

    @Test
    void listVersionsUsesCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiPortalResourceService service = mock(BiPortalResourceService.class);
        BiPortalVersionView version = new BiPortalVersionView(
                61L,
                "canvas-ops-portal",
                2,
                "PUBLISHED",
                portal("PUBLISHED"),
                "alice",
                null);
        when(service.listVersions(7L, "canvas-ops-portal", 5)).thenReturn(List.of(version));
        BiPortalController controller = new BiPortalController(resolver, service);

        StepVerifier.create(controller.listVersions("canvas-ops-portal", 5))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.version()).isEqualTo(2)))
                .verifyComplete();

        verify(service).listVersions(7L, "canvas-ops-portal", 5);
    }

    @Test
    void restoreVersionUsesCurrentTenantUserAndLockToken() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiPortalResourceService service = mock(BiPortalResourceService.class);
        when(service.restoreVersion(7L, "alice", "TENANT_ADMIN", "canvas-ops-portal", 2, "lock-token-1"))
                .thenReturn(portal("DRAFT"));
        BiPortalController controller = new BiPortalController(resolver, service);

        StepVerifier.create(controller.restoreVersion("canvas-ops-portal", "lock-token-1", 2))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("DRAFT"))
                .verifyComplete();

        verify(service).restoreVersion(7L, "alice", "TENANT_ADMIN", "canvas-ops-portal", 2, "lock-token-1");
    }

    private BiPortalResource portal(String status) {
        return new BiPortalResource(
                "canvas-ops-portal",
                "Canvas Operations Portal",
                Map.of("theme", "light"),
                List.of(new BiPortalMenuResource("overview", null, "经营总览", "DASHBOARD", "canvas-effect", 21L, null, Map.of(), 10)),
                status,
                "PERSISTED");
    }
}
