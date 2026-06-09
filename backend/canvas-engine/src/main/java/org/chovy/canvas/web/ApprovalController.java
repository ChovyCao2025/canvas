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

/**
 * ApprovalController 业务组件。
 */
@RestController
@RequestMapping("/approvals")
public class ApprovalController {

    private final TenantContextResolver tenantContextResolver;
    private final ApprovalWorkflowService workflowService;

    /**
     * 执行 ApprovalController 流程，围绕 approval controller 完成校验、计算或结果组装。
     *
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param workflowService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public ApprovalController(TenantContextResolver tenantContextResolver,
                              ApprovalWorkflowService workflowService) {
        this.tenantContextResolver = tenantContextResolver;
        this.workflowService = workflowService;
    }

    /**
     * 执行 tasks 流程，围绕 tasks 完成校验、计算或结果组装。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回 tasks 汇总后的集合、分页或映射视图。
     */
    @GetMapping("/tasks")
    public Mono<R<List<ApprovalTaskView>>> tasks(
            @RequestParam(defaultValue = ApprovalWorkflowService.STATUS_PENDING) String status) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> R.ok(workflowService.listTasks(
                                tenantId(context), username(context), context.role(), status)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 执行 instances 流程，围绕 instances 完成校验、计算或结果组装。
     *
     * @param targetType 类型标识，用于选择对应处理分支。
     * @param targetId 业务对象 ID，用于定位具体记录。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回 instances 汇总后的集合、分页或映射视图。
     */
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

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param taskId 业务对象 ID，用于定位具体记录。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    @PostMapping("/tasks/{taskId}/approve")
    public Mono<R<ApprovalInstanceView>> approve(
            @PathVariable Long taskId,
            @RequestBody(required = false) ApprovalDecisionRequest request) {
        return decide(taskId, request, true);
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param taskId 业务对象 ID，用于定位具体记录。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 reject 流程生成的业务结果。
     */
    @PostMapping("/tasks/{taskId}/reject")
    public Mono<R<ApprovalInstanceView>> reject(
            @PathVariable Long taskId,
            @RequestBody(required = false) ApprovalDecisionRequest request) {
        return decide(taskId, request, false);
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回流程执行后的业务结果。
     */
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

    /**
     * 执行核心业务处理流程。
     *
     * @param instanceId 业务对象 ID，用于定位具体记录。
     * @return 返回流程执行后的业务结果。
     */
    @PostMapping("/external/lark/instances/{instanceId}/sync")
    public Mono<R<ApprovalInstanceView>> syncLarkApprovalInstance(@PathVariable Long instanceId) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> {
                            requireAdmin(context);
                            return R.ok(workflowService.syncExternalInstance(tenantId(context), instanceId));
                        })
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param taskId 业务对象 ID，用于定位具体记录。
     * @param request 请求对象，承载本次操作的输入参数。
     * @param approve approve 参数，用于 decide 流程中的校验、计算或对象转换。
     * @return 返回 decide 流程生成的业务结果。
     */
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

    /**
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 currentTenant 流程生成的业务结果。
     */
    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.current()
                .switchIfEmpty(Mono.error(new SecurityException("AUTH_003: missing tenant context")));
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private Long tenantId(TenantContext context) {
        return context.tenantId() == null ? 0L : context.tenantId();
    }

    /**
     * 解析操作人标识。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 username 生成的文本或业务键。
     */
    private String username(TenantContext context) {
        return context.username() == null || context.username().isBlank()
                ? "system"
                : context.username();
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     */
    private void requireAdmin(TenantContext context) {
        String role = context == null ? null : context.role();
        if (RoleNames.TENANT_ADMIN.equalsIgnoreCase(role)
                || RoleNames.ADMIN.equalsIgnoreCase(role)
                || RoleNames.SUPER_ADMIN.equalsIgnoreCase(role)) {
            return;
        }
        throw new AccessDeniedException("Lark approval sync requires admin role");
    }

    /**
     * ApprovalExternalSyncResponse 数据记录。
     */
    public record ApprovalExternalSyncResponse(int synced) {
    }
}
