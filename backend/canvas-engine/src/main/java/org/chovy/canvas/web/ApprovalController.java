package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.approval.ApprovalDecisionCommand;
import org.chovy.canvas.domain.approval.ApprovalDecisionRequest;
import org.chovy.canvas.domain.approval.ApprovalInstanceView;
import org.chovy.canvas.domain.approval.ApprovalTaskView;
import org.chovy.canvas.domain.approval.ApprovalWorkflowService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.AccessDeniedException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/approvals")
public class ApprovalController {

    private final TenantContextResolver tenantContextResolver;
    private final ApprovalWorkflowService workflowService;

    public ApprovalController(TenantContextResolver tenantContextResolver,
                              ApprovalWorkflowService workflowService) {
        this.tenantContextResolver = tenantContextResolver;
        this.workflowService = workflowService;
    }

    @GetMapping("/tasks")
    public Mono<R<List<ApprovalTaskView>>> tasks(
            @RequestParam(defaultValue = ApprovalWorkflowService.STATUS_PENDING) String status) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> R.ok(workflowService.listTasks(
                                tenantId(context), username(context), context.role(), status)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/instances")
    public Mono<R<List<ApprovalInstanceView>>> instances(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String status) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> R.ok(workflowService.listInstances(
                                tenantId(context), targetType, targetId, status)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/tasks/{taskId}/approve")
    public Mono<R<ApprovalInstanceView>> approve(
            @PathVariable Long taskId,
            @RequestBody(required = false) ApprovalDecisionRequest request) {
        return decide(taskId, request, true);
    }

    @PostMapping("/tasks/{taskId}/reject")
    public Mono<R<ApprovalInstanceView>> reject(
            @PathVariable Long taskId,
            @RequestBody(required = false) ApprovalDecisionRequest request) {
        return decide(taskId, request, false);
    }

    @PostMapping("/external/lark/sync")
    public Mono<R<ApprovalExternalSyncResponse>> syncLarkApprovals(
            @RequestParam(defaultValue = "100") int limit) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> {
                            requireAdmin(context);
                            int synced = workflowService.syncPendingExternalInstances(tenantId(context), limit);
                            return R.ok(new ApprovalExternalSyncResponse(synced));
                        })
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<R<ApprovalInstanceView>> decide(Long taskId, ApprovalDecisionRequest request, boolean approve) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> {
                            ApprovalDecisionCommand command = new ApprovalDecisionCommand(
                                    tenantId(context),
                                    taskId,
                                    username(context),
                                    context.role(),
                                    request == null ? null : request.comment());
                            return R.ok(approve
                                    ? workflowService.approveTask(command)
                                    : workflowService.rejectTask(command));
                        })
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.current()
                .switchIfEmpty(Mono.error(new SecurityException("AUTH_003: missing tenant context")));
    }

    private Long tenantId(TenantContext context) {
        return context.tenantId() == null ? 0L : context.tenantId();
    }

    private String username(TenantContext context) {
        return context.username() == null || context.username().isBlank()
                ? "system"
                : context.username();
    }

    private void requireAdmin(TenantContext context) {
        String role = context == null ? null : context.role();
        if (RoleNames.TENANT_ADMIN.equalsIgnoreCase(role)
                || RoleNames.ADMIN.equalsIgnoreCase(role)
                || RoleNames.SUPER_ADMIN.equalsIgnoreCase(role)) {
            return;
        }
        throw new AccessDeniedException("Lark approval sync requires admin role");
    }

    public record ApprovalExternalSyncResponse(int synced) {
    }
}
