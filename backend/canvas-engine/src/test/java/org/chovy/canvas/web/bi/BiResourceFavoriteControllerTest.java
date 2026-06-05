package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.resource.BiResourceFavoriteCommand;
import org.chovy.canvas.domain.bi.resource.BiResourceFavoriteService;
import org.chovy.canvas.domain.bi.resource.BiResourceFavoriteView;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiResourceFavoriteControllerTest {

    @Test
    void routeUsesCanvasBiApiPrefix() {
        RequestMapping mapping = BiResourceFavoriteController.class.getAnnotation(RequestMapping.class);

        assertThat(mapping.value()).containsExactly("/canvas/bi/resources");
    }

    @Test
    void favoriteRouteUsesFavoritesEndpoint() throws Exception {
        PostMapping mapping = BiResourceFavoriteController.class
                .getMethod("favorite", BiResourceFavoriteCommand.class)
                .getAnnotation(PostMapping.class);

        assertThat(mapping.value()).containsExactly("/favorites");
    }

    @Test
    void listRouteUsesFavoritesEndpoint() throws Exception {
        GetMapping mapping = BiResourceFavoriteController.class
                .getMethod("list", String.class)
                .getAnnotation(GetMapping.class);

        assertThat(mapping.value()).containsExactly("/favorites");
    }

    @Test
    void favoriteUsesCurrentTenantAndUser() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiResourceFavoriteService service = mock(BiResourceFavoriteService.class);
        BiResourceFavoriteCommand command = new BiResourceFavoriteCommand(
                "DASHBOARD",
                "canvas-effect");
        BiResourceFavoriteView view = new BiResourceFavoriteView(
                42L,
                7L,
                5L,
                "DASHBOARD",
                "canvas-effect",
                "alice",
                LocalDateTime.parse("2026-06-05T12:00:00"));
        when(service.favorite(7L, "alice", command)).thenReturn(view);
        BiResourceFavoriteController controller = new BiResourceFavoriteController(resolver, service);

        StepVerifier.create(controller.favorite(command))
                .assertNext(response -> assertThat(response.getData().username()).isEqualTo("alice"))
                .verifyComplete();

        verify(service).favorite(7L, "alice", command);
    }

    @Test
    void listUsesCurrentTenantUserAndResourceTypeFilter() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiResourceFavoriteService service = mock(BiResourceFavoriteService.class);
        when(service.list(7L, "alice", "DASHBOARD")).thenReturn(List.of());
        BiResourceFavoriteController controller = new BiResourceFavoriteController(resolver, service);

        StepVerifier.create(controller.list("DASHBOARD"))
                .assertNext(response -> assertThat(response.getData()).isEmpty())
                .verifyComplete();

        verify(service).list(7L, "alice", "DASHBOARD");
    }
}
