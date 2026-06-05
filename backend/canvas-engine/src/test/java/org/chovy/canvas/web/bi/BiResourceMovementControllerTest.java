package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.resource.BiResourceLocationView;
import org.chovy.canvas.domain.bi.resource.BiResourceMoveCommand;
import org.chovy.canvas.domain.bi.resource.BiResourceMovementService;
import org.junit.jupiter.api.Test;
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

class BiResourceMovementControllerTest {

    @Test
    void routeUsesCanvasBiApiPrefix() {
        RequestMapping mapping = BiResourceMovementController.class.getAnnotation(RequestMapping.class);

        assertThat(mapping.value()).containsExactly("/canvas/bi/resources");
    }

    @Test
    void moveRouteUsesDocumentedLocationEndpoint() throws Exception {
        PostMapping mapping = BiResourceMovementController.class
                .getMethod("move", BiResourceMoveCommand.class)
                .getAnnotation(PostMapping.class);

        assertThat(mapping.value()).contains("/locations");
    }

    @Test
    void moveUsesCurrentTenantAndUser() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiResourceMovementService service = mock(BiResourceMovementService.class);
        BiResourceMoveCommand command = new BiResourceMoveCommand(
                "DASHBOARD",
                "canvas-effect",
                "operations/q2",
                30);
        BiResourceLocationView view = new BiResourceLocationView(
                42L,
                7L,
                5L,
                "DASHBOARD",
                "canvas-effect",
                "operations/q2",
                30,
                "alice",
                LocalDateTime.parse("2026-06-05T10:30:00"));
        when(service.move(7L, "alice", command)).thenReturn(view);
        BiResourceMovementController controller = new BiResourceMovementController(resolver, service);

        StepVerifier.create(controller.move(command))
                .assertNext(response -> assertThat(response.getData().folderKey()).isEqualTo("operations/q2"))
                .verifyComplete();

        verify(service).move(7L, "alice", command);
    }

    @Test
    void listUsesCurrentTenantAndResourceTypeFilter() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiResourceMovementService service = mock(BiResourceMovementService.class);
        when(service.list(7L, "DASHBOARD")).thenReturn(List.of());
        BiResourceMovementController controller = new BiResourceMovementController(resolver, service);

        StepVerifier.create(controller.list("DASHBOARD"))
                .assertNext(response -> assertThat(response.getData()).isEmpty())
                .verifyComplete();

        verify(service).list(7L, "DASHBOARD");
    }
}
