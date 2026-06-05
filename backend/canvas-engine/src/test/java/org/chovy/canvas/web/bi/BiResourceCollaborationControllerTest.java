package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.resource.BiResourceCollaborationService;
import org.chovy.canvas.domain.bi.resource.BiResourceCommentCommand;
import org.chovy.canvas.domain.bi.resource.BiResourceCommentView;
import org.chovy.canvas.domain.bi.resource.BiResourceLockCommand;
import org.chovy.canvas.domain.bi.resource.BiResourceLockView;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
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

class BiResourceCollaborationControllerTest {

    @Test
    void routeUsesCanvasBiResourcePrefix() {
        RequestMapping mapping = BiResourceCollaborationController.class.getAnnotation(RequestMapping.class);

        assertThat(mapping.value()).containsExactly("/canvas/bi/resources");
    }

    @Test
    void routesExposeCommentsAndLocks() throws Exception {
        assertThat(BiResourceCollaborationController.class
                .getMethod("addComment", BiResourceCommentCommand.class)
                .getAnnotation(PostMapping.class).value()).containsExactly("/comments");
        assertThat(BiResourceCollaborationController.class
                .getMethod("listComments", String.class, String.class)
                .getAnnotation(GetMapping.class).value()).containsExactly("/comments");
        assertThat(BiResourceCollaborationController.class
                .getMethod("deleteComment", Long.class)
                .getAnnotation(DeleteMapping.class).value()).containsExactly("/comments/{commentId}");
        assertThat(BiResourceCollaborationController.class
                .getMethod("acquireLock", BiResourceLockCommand.class)
                .getAnnotation(PostMapping.class).value()).containsExactly("/locks/acquire");
        assertThat(BiResourceCollaborationController.class
                .getMethod("currentLock", String.class, String.class)
                .getAnnotation(GetMapping.class).value()).containsExactly("/locks");
        assertThat(BiResourceCollaborationController.class
                .getMethod("releaseLock", BiResourceLockCommand.class)
                .getAnnotation(PostMapping.class).value()).containsExactly("/locks/release");
    }

    @Test
    void addCommentUsesCurrentTenantAndUser() {
        TenantContextResolver resolver = resolver();
        BiResourceCollaborationService service = mock(BiResourceCollaborationService.class);
        BiResourceCommentCommand command = new BiResourceCommentCommand(
                "DASHBOARD",
                "canvas-effect",
                "kpi-total",
                "已确认");
        BiResourceCommentView view = new BiResourceCommentView(
                9L,
                7L,
                5L,
                "DASHBOARD",
                "canvas-effect",
                "kpi-total",
                "已确认",
                "alice",
                LocalDateTime.parse("2026-06-05T12:00:00"),
                null);
        when(service.addComment(7L, "alice", command)).thenReturn(view);
        BiResourceCollaborationController controller = new BiResourceCollaborationController(resolver, service);

        StepVerifier.create(controller.addComment(command))
                .assertNext(response -> assertThat(response.getData().commentText()).isEqualTo("已确认"))
                .verifyComplete();

        verify(service).addComment(7L, "alice", command);
    }

    @Test
    void acquireLockUsesCurrentTenantAndUser() {
        TenantContextResolver resolver = resolver();
        BiResourceCollaborationService service = mock(BiResourceCollaborationService.class);
        BiResourceLockCommand command = new BiResourceLockCommand(
                "DASHBOARD",
                "canvas-effect",
                "token-1",
                120);
        BiResourceLockView view = new BiResourceLockView(
                null,
                7L,
                5L,
                "DASHBOARD",
                "canvas-effect",
                "token-1",
                "alice",
                LocalDateTime.parse("2026-06-05T12:00:00"),
                LocalDateTime.parse("2026-06-05T12:02:00"),
                true);
        when(service.acquireLock(7L, "alice", command)).thenReturn(view);
        BiResourceCollaborationController controller = new BiResourceCollaborationController(resolver, service);

        StepVerifier.create(controller.acquireLock(command))
                .assertNext(response -> assertThat(response.getData().locked()).isTrue())
                .verifyComplete();

        verify(service).acquireLock(7L, "alice", command);
    }

    @Test
    void listCommentsUsesResourceFilter() {
        TenantContextResolver resolver = resolver();
        BiResourceCollaborationService service = mock(BiResourceCollaborationService.class);
        when(service.listComments(7L, "DASHBOARD", "canvas-effect")).thenReturn(List.of());
        BiResourceCollaborationController controller = new BiResourceCollaborationController(resolver, service);

        StepVerifier.create(controller.listComments("DASHBOARD", "canvas-effect"))
                .assertNext(response -> assertThat(response.getData()).isEmpty())
                .verifyComplete();

        verify(service).listComments(7L, "DASHBOARD", "canvas-effect");
    }

    @Test
    void currentLockUsesCurrentTenantAndResourceFilter() {
        TenantContextResolver resolver = resolver();
        BiResourceCollaborationService service = mock(BiResourceCollaborationService.class);
        BiResourceLockView view = new BiResourceLockView(
                15L,
                7L,
                5L,
                "DASHBOARD",
                "canvas-effect",
                "token-1",
                "alice",
                LocalDateTime.parse("2026-06-05T12:00:00"),
                LocalDateTime.parse("2026-06-05T12:02:00"),
                true);
        when(service.currentLock(7L, "DASHBOARD", "canvas-effect")).thenReturn(view);
        BiResourceCollaborationController controller = new BiResourceCollaborationController(resolver, service);

        StepVerifier.create(controller.currentLock("DASHBOARD", "canvas-effect"))
                .assertNext(response -> assertThat(response.getData().locked()).isTrue())
                .verifyComplete();

        verify(service).currentLock(7L, "DASHBOARD", "canvas-effect");
    }

    @Test
    void releaseLockUsesCurrentTenantAndUser() {
        TenantContextResolver resolver = resolver();
        BiResourceCollaborationService service = mock(BiResourceCollaborationService.class);
        BiResourceLockCommand command = new BiResourceLockCommand(
                "DASHBOARD",
                "canvas-effect",
                "token-1",
                null);
        BiResourceCollaborationController controller = new BiResourceCollaborationController(resolver, service);

        StepVerifier.create(controller.releaseLock(command))
                .assertNext(response -> assertThat(response.getData()).isNull())
                .verifyComplete();

        verify(service).releaseLock(7L, "alice", command);
    }

    private TenantContextResolver resolver() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        return resolver;
    }
}
