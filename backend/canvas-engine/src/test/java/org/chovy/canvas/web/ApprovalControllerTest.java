package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.approval.ApprovalDecisionCommand;
import org.chovy.canvas.domain.approval.ApprovalDecisionRequest;
import org.chovy.canvas.domain.approval.ApprovalInstanceView;
import org.chovy.canvas.domain.approval.ApprovalTaskView;
import org.chovy.canvas.domain.approval.ApprovalWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalControllerTest {

    @Test
    void listTasksUsesCurrentTenantActorAndRole() {
        ApprovalWorkflowService service = mock(ApprovalWorkflowService.class);
        ApprovalController controller = new ApprovalController(resolver(), service);
        when(service.listTasks(7L, "alice", RoleNames.TENANT_ADMIN, "PENDING"))
                .thenReturn(List.of(task()));

        StepVerifier.create(controller.tasks("PENDING"))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(task -> assertThat(task.id()).isEqualTo(201L)))
                .verifyComplete();

        verify(service).listTasks(7L, "alice", RoleNames.TENANT_ADMIN, "PENDING");
    }

    @Test
    void approveTaskCopiesPathTaskIdAndCurrentActor() {
        ApprovalWorkflowService service = mock(ApprovalWorkflowService.class);
        ApprovalController controller = new ApprovalController(resolver(), service);
        ApprovalDecisionRequest request = new ApprovalDecisionRequest("looks good");
        when(service.approveTask(new ApprovalDecisionCommand(
                7L, 201L, "alice", RoleNames.TENANT_ADMIN, "looks good")))
                .thenReturn(instance("APPROVED"));

        StepVerifier.create(controller.approve(201L, request))
                .assertNext(response -> {
                    assertThat(response.getData().status()).isEqualTo("APPROVED");
                    assertThat(response.getData().externalInstanceId()).isEqualTo("lark-instance-101");
                    assertThat(response.getData().pendingTasks()).singleElement()
                            .satisfies(task -> assertThat(task.externalTaskId()).isEqualTo("lark-task-201"));
                })
                .verifyComplete();

        verify(service).approveTask(new ApprovalDecisionCommand(
                7L, 201L, "alice", RoleNames.TENANT_ADMIN, "looks good"));
    }

    @Test
    void rejectTaskCopiesPathTaskIdAndComment() {
        ApprovalWorkflowService service = mock(ApprovalWorkflowService.class);
        ApprovalController controller = new ApprovalController(resolver(), service);
        ApprovalDecisionRequest request = new ApprovalDecisionRequest("risk unclear");
        when(service.rejectTask(new ApprovalDecisionCommand(
                7L, 201L, "alice", RoleNames.TENANT_ADMIN, "risk unclear")))
                .thenReturn(instance("REJECTED"));

        StepVerifier.create(controller.reject(201L, request))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("REJECTED"))
                .verifyComplete();

        verify(service).rejectTask(new ApprovalDecisionCommand(
                7L, 201L, "alice", RoleNames.TENANT_ADMIN, "risk unclear"));
    }

    @Test
    void syncLarkApprovalsRequiresTenantAdminAndDelegatesBoundedLimit() {
        ApprovalWorkflowService service = mock(ApprovalWorkflowService.class);
        ApprovalController controller = new ApprovalController(resolver(RoleNames.TENANT_ADMIN), service);
        when(service.syncPendingExternalInstances(7L, 25)).thenReturn(3);

        StepVerifier.create(controller.syncLarkApprovals(25))
                .assertNext(response -> assertThat(response.getData().synced()).isEqualTo(3))
                .verifyComplete();

        verify(service).syncPendingExternalInstances(7L, 25);
    }

    @Test
    void syncLarkApprovalsRejectsOperatorRole() {
        ApprovalWorkflowService service = mock(ApprovalWorkflowService.class);
        ApprovalController controller = new ApprovalController(resolver(RoleNames.OPERATOR), service);

        StepVerifier.create(controller.syncLarkApprovals(25))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(AccessDeniedException.class)
                        .hasMessageContaining("admin role"))
                .verify();

        verify(service, never()).syncPendingExternalInstances(7L, 25);
    }

    @Test
    void syncSingleLarkApprovalRequiresTenantAdminAndDelegatesInstanceId() {
        ApprovalWorkflowService service = mock(ApprovalWorkflowService.class);
        ApprovalController controller = new ApprovalController(resolver(RoleNames.TENANT_ADMIN), service);
        when(service.syncExternalInstance(7L, 101L)).thenReturn(instance("APPROVED"));

        StepVerifier.create(controller.syncLarkApprovalInstance(101L))
                .assertNext(response -> {
                    assertThat(response.getData().id()).isEqualTo(101L);
                    assertThat(response.getData().status()).isEqualTo("APPROVED");
                })
                .verifyComplete();

        verify(service).syncExternalInstance(7L, 101L);
    }

    @Test
    void syncSingleLarkApprovalRejectsOperatorRole() {
        ApprovalWorkflowService service = mock(ApprovalWorkflowService.class);
        ApprovalController controller = new ApprovalController(resolver(RoleNames.OPERATOR), service);

        StepVerifier.create(controller.syncLarkApprovalInstance(101L))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(AccessDeniedException.class)
                        .hasMessageContaining("admin role"))
                .verify();

        verify(service, never()).syncExternalInstance(7L, 101L);
    }

    private TenantContextResolver resolver() {
        return resolver(RoleNames.TENANT_ADMIN);
    }

    private TenantContextResolver resolver(String role) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, role, "alice")));
        return resolver;
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

    private ApprovalInstanceView instance(String status) {
        return new ApprovalInstanceView(
                101L,
                7L,
                "CANVAS_PUBLISH_DEFAULT",
                "CANVAS",
                "CANVAS",
                "62",
                91L,
                status,
                "alice",
                "准备发布",
                "HIGH",
                "[]",
                "{}",
                "lark-instance-101",
                LocalDateTime.parse("2026-06-06T12:00:00"),
                null,
                null,
                null,
                "PUBLISH_CANVAS",
                null,
                null,
                List.of(new ApprovalTaskView(
                        201L,
                        7L,
                        101L,
                        1,
                        "alice",
                        "PENDING",
                        "lark-task-201",
                        LocalDateTime.parse("2026-06-06T13:00:00"),
                        null,
                        null)));
    }
}
