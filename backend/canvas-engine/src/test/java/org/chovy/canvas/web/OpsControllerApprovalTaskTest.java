package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.mapper.CanvasManualApprovalMapper;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasTemplateMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.domain.approval.ApprovalTaskView;
import org.chovy.canvas.domain.approval.ApprovalWorkflowService;
import org.chovy.canvas.domain.canvas.CanvasOpsService;
import org.chovy.canvas.domain.canvas.CanvasService;
import org.chovy.canvas.domain.notification.NotificationEventService;
import org.chovy.canvas.domain.ops.OpsAuditEventService;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.chovy.canvas.infrastructure.redis.TriggerRouteRecoveryService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpsControllerApprovalTaskTest {

    @Test
    void pendingReviewsReadsUnifiedApprovalTasksWhenWorkflowServiceIsConfigured() {
        ApprovalWorkflowService workflowService = mock(ApprovalWorkflowService.class);
        OpsController controller = controller();
        controller.setApprovalWorkflowService(workflowService);
        when(workflowService.listTasks(7L, "alice", RoleNames.TENANT_ADMIN, "PENDING"))
                .thenReturn(List.of(task()));

        StepVerifier.create(controller.pendingReviews())
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(task -> assertThat(task.id()).isEqualTo(201L)))
                .verifyComplete();

        verify(workflowService).listTasks(7L, "alice", RoleNames.TENANT_ADMIN, "PENDING");
    }

    private OpsController controller() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(new TenantContext(7L, RoleNames.TENANT_ADMIN, "alice")));
        return new OpsController(
                mock(CanvasTemplateMapper.class),
                mock(CanvasMapper.class),
                mock(CanvasVersionMapper.class),
                mock(CanvasManualApprovalMapper.class),
                mock(CanvasConfigCache.class),
                mock(TriggerRouteRecoveryService.class),
                resolver,
                mock(CanvasService.class),
                mock(CanvasOpsService.class),
                mock(OpsAuditEventService.class),
                mock(NotificationEventService.class));
    }

    private ApprovalTaskView task() {
        return new ApprovalTaskView(
                201L,
                7L,
                101L,
                1,
                "tenant_admin",
                "PENDING",
                null,
                LocalDateTime.parse("2026-06-06T13:00:00"),
                null,
                null);
    }
}
