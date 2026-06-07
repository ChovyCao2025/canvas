package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.domain.approval.ApprovalInstanceView;
import org.chovy.canvas.domain.approval.CanvasPublishApprovalRequest;
import org.chovy.canvas.domain.approval.CanvasPublishApprovalService;
import org.chovy.canvas.domain.approval.CanvasPublishApprovalStatusView;
import org.chovy.canvas.domain.canvas.CanvasImportExportService;
import org.chovy.canvas.domain.canvas.CanvasMessagePreviewService;
import org.chovy.canvas.domain.canvas.CanvasOpsService;
import org.chovy.canvas.domain.canvas.CanvasPrePublishCheckService;
import org.chovy.canvas.domain.canvas.CanvasProjectFolderMetadataService;
import org.chovy.canvas.domain.canvas.CanvasService;
import org.chovy.canvas.domain.notification.NotificationEventService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasControllerApprovalTest {

    @Test
    void submitReviewDelegatesToCanvasPublishApprovalService() {
        CanvasPublishApprovalService approvalService = mock(CanvasPublishApprovalService.class);
        CanvasController controller = controller(mock(CanvasService.class), approvalService);
        CanvasPublishApprovalRequest request = new CanvasPublishApprovalRequest("准备发布活动");
        when(approvalService.submitReview(10L, 62L, "alice", request)).thenReturn(instanceView("PENDING"));

        StepVerifier.create(controller.submitReview(62L, request))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("PENDING"))
                .verifyComplete();

        verify(approvalService).submitReview(10L, 62L, "alice", request);
    }

    @Test
    void approvalStatusDelegatesToCanvasPublishApprovalService() {
        CanvasPublishApprovalService approvalService = mock(CanvasPublishApprovalService.class);
        CanvasController controller = controller(mock(CanvasService.class), approvalService);
        CanvasPublishApprovalStatusView status = new CanvasPublishApprovalStatusView(
                62L, 91L, true, "HIGH", List.of("PROJECT_REQUIRES_REVIEW"), null);
        when(approvalService.approvalStatus(10L, 62L)).thenReturn(status);

        StepVerifier.create(controller.approvalStatus(62L))
                .assertNext(response -> assertThat(response.getData().approvalRequired()).isTrue())
                .verifyComplete();

        verify(approvalService).approvalStatus(10L, 62L);
    }

    @Test
    void publishUsesApprovalGateWhenApprovalServiceIsConfigured() {
        CanvasService canvasService = mock(CanvasService.class);
        CanvasPublishApprovalService approvalService = mock(CanvasPublishApprovalService.class);
        CanvasController controller = controller(canvasService, approvalService);
        CanvasVersionDO version = new CanvasVersionDO();
        version.setId(99L);
        when(approvalService.publishWithApprovalGate(10L, 62L, "alice")).thenReturn(version);

        StepVerifier.create(controller.publish(62L, "alice"))
                .assertNext(response -> assertThat(response.getData()).isSameAs(version))
                .verifyComplete();

        verify(approvalService).publishWithApprovalGate(10L, 62L, "alice");
        verify(canvasService, never()).publish(62L, "alice");
    }

    private CanvasController controller(CanvasService canvasService,
                                        CanvasPublishApprovalService approvalService) {
        TenantContextResolver tenantResolver = mock(TenantContextResolver.class);
        when(tenantResolver.current()).thenReturn(Mono.just(new TenantContext(
                10L, RoleNames.TENANT_ADMIN, "alice")));
        CanvasController controller = new CanvasController(
                canvasService,
                mock(CanvasOpsService.class),
                mock(NotificationEventService.class),
                tenantResolver,
                mock(CanvasMessagePreviewService.class),
                mock(CanvasImportExportService.class),
                mock(CanvasProjectFolderMetadataService.class),
                mock(CanvasPrePublishCheckService.class));
        controller.setCanvasPublishApprovalService(approvalService);
        return controller;
    }

    private ApprovalInstanceView instanceView(String status) {
        return new ApprovalInstanceView(
                101L,
                10L,
                "CANVAS_PUBLISH_DEFAULT",
                "CANVAS",
                "CANVAS",
                "62",
                91L,
                status,
                "alice",
                "准备发布活动",
                "HIGH",
                "[\"PROJECT_REQUIRES_REVIEW\"]",
                "{\"canvasId\":62}",
                null,
                LocalDateTime.parse("2026-06-06T12:06:00"),
                null,
                null,
                null,
                "PUBLISH_CANVAS",
                null,
                null,
                List.of());
    }
}
