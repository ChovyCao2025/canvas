package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.resource.BiResourceOwnershipView;
import org.chovy.canvas.domain.bi.resource.BiResourceTransferCommand;
import org.chovy.canvas.domain.bi.resource.BiResourceTransferService;
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

class BiResourceTransferControllerTest {

    @Test
    void routeUsesCanvasBiApiPrefix() {
        RequestMapping mapping = BiResourceTransferController.class.getAnnotation(RequestMapping.class);

        assertThat(mapping.value()).containsExactly("/canvas/bi/resources");
    }

    @Test
    void transferRouteUsesDocumentedEndpoint() throws Exception {
        PostMapping mapping = BiResourceTransferController.class
                .getMethod("transfer", BiResourceTransferCommand.class)
                .getAnnotation(PostMapping.class);

        assertThat(mapping.value()).containsExactly("/transfer");
    }

    @Test
    void listRouteUsesOwnershipEndpoint() throws Exception {
        GetMapping mapping = BiResourceTransferController.class
                .getMethod("list", String.class)
                .getAnnotation(GetMapping.class);

        assertThat(mapping.value()).containsExactly("/ownerships");
    }

    @Test
    void transferUsesCurrentTenantAndUser() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiResourceTransferService service = mock(BiResourceTransferService.class);
        BiResourceTransferCommand command = new BiResourceTransferCommand(
                "DASHBOARD",
                "canvas-effect",
                "bob");
        BiResourceOwnershipView view = new BiResourceOwnershipView(
                42L,
                7L,
                5L,
                "DASHBOARD",
                "canvas-effect",
                "bob",
                "alice",
                LocalDateTime.parse("2026-06-05T11:30:00"));
        when(service.transfer(7L, "alice", command)).thenReturn(view);
        BiResourceTransferController controller = new BiResourceTransferController(resolver, service);

        StepVerifier.create(controller.transfer(command))
                .assertNext(response -> assertThat(response.getData().ownerUser()).isEqualTo("bob"))
                .verifyComplete();

        verify(service).transfer(7L, "alice", command);
    }

    @Test
    void listUsesCurrentTenantAndResourceTypeFilter() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiResourceTransferService service = mock(BiResourceTransferService.class);
        when(service.list(7L, "DASHBOARD")).thenReturn(List.of());
        BiResourceTransferController controller = new BiResourceTransferController(resolver, service);

        StepVerifier.create(controller.list("DASHBOARD"))
                .assertNext(response -> assertThat(response.getData()).isEmpty())
                .verifyComplete();

        verify(service).list(7L, "DASHBOARD");
    }
}
