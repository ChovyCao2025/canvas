package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.resource.BiPublishApprovalRequestCommand;
import org.chovy.canvas.domain.bi.resource.BiPublishApprovalReviewCommand;
import org.chovy.canvas.domain.bi.resource.BiPublishApprovalService;
import org.chovy.canvas.domain.bi.resource.BiPublishApprovalView;
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

class BiPublishApprovalControllerTest {

    @Test
    void routeUsesCanvasBiResourcePrefix() {
        RequestMapping mapping = BiPublishApprovalController.class.getAnnotation(RequestMapping.class);

        assertThat(mapping.value()).containsExactly("/canvas/bi/resources/publish-approvals");
    }

    @Test
    void routesExposeListRequestAndReview() throws Exception {
        assertThat(BiPublishApprovalController.class
                .getMethod("list", String.class, String.class, String.class)
                .getAnnotation(GetMapping.class).value()).containsExactly("");
        assertThat(BiPublishApprovalController.class
                .getMethod("requestApproval", BiPublishApprovalRequestCommand.class)
                .getAnnotation(PostMapping.class).value()).containsExactly("");
        assertThat(BiPublishApprovalController.class
                .getMethod("reviewApproval", Long.class, BiPublishApprovalReviewCommand.class)
                .getAnnotation(PostMapping.class).value()).containsExactly("/{approvalId}/review");
    }

    @Test
    void requestApprovalUsesCurrentTenantAndUser() {
        TenantContextResolver resolver = resolver();
        BiPublishApprovalService service = mock(BiPublishApprovalService.class);
        BiPublishApprovalRequestCommand command = new BiPublishApprovalRequestCommand(
                "DASHBOARD",
                "canvas-effect",
                "准备发布");
        BiPublishApprovalView view = view("PENDING");
        when(service.requestApproval(7L, "alice", command)).thenReturn(view);
        BiPublishApprovalController controller = new BiPublishApprovalController(resolver, service);

        StepVerifier.create(controller.requestApproval(command))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("PENDING"))
                .verifyComplete();

        verify(service).requestApproval(7L, "alice", command);
    }

    @Test
    void reviewApprovalCopiesPathIdAndUsesCurrentReviewer() {
        TenantContextResolver resolver = resolver();
        BiPublishApprovalService service = mock(BiPublishApprovalService.class);
        BiPublishApprovalReviewCommand command = new BiPublishApprovalReviewCommand(null, "approved", "OK");
        when(service.reviewApproval(7L, "alice", new BiPublishApprovalReviewCommand(9L, "approved", "OK")))
                .thenReturn(view("APPROVED"));
        BiPublishApprovalController controller = new BiPublishApprovalController(resolver, service);

        StepVerifier.create(controller.reviewApproval(9L, command))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("APPROVED"))
                .verifyComplete();

        verify(service).reviewApproval(7L, "alice", new BiPublishApprovalReviewCommand(9L, "approved", "OK"));
    }

    @Test
    void listUsesCurrentTenantAndFilters() {
        TenantContextResolver resolver = resolver();
        BiPublishApprovalService service = mock(BiPublishApprovalService.class);
        when(service.listApprovals(7L, "DASHBOARD", "canvas-effect", "PENDING")).thenReturn(List.of());
        BiPublishApprovalController controller = new BiPublishApprovalController(resolver, service);

        StepVerifier.create(controller.list("DASHBOARD", "canvas-effect", "PENDING"))
                .assertNext(response -> assertThat(response.getData()).isEmpty())
                .verifyComplete();

        verify(service).listApprovals(7L, "DASHBOARD", "canvas-effect", "PENDING");
    }

    private TenantContextResolver resolver() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        return resolver;
    }

    private BiPublishApprovalView view(String status) {
        return new BiPublishApprovalView(
                9L,
                7L,
                5L,
                "DASHBOARD",
                "canvas-effect",
                status,
                "准备发布",
                "alice",
                LocalDateTime.parse("2026-06-05T13:00:00"),
                null,
                null,
                null);
    }
}
