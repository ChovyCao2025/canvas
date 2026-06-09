package org.chovy.canvas.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.domain.canvas.CanvasService;
import org.chovy.canvas.domain.project.CanvasProjectAction;
import org.chovy.canvas.domain.project.CanvasProjectPermissionService;
import org.chovy.canvas.domain.project.CanvasProjectService;
import org.chovy.canvas.dto.project.ProjectCreateReq;
import org.chovy.canvas.dto.project.ProjectDetailResp;
import org.chovy.canvas.dto.project.ProjectMemberResp;
import org.chovy.canvas.dto.project.ProjectMemberUpdateReq;
import org.chovy.canvas.dto.project.ProjectStatsResp;
import org.chovy.canvas.dto.project.ProjectSummaryResp;
import org.chovy.canvas.dto.project.ProjectUpdateReq;
import org.chovy.canvas.query.CanvasListQuery;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * CanvasProjectController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/admin/projects")
@RequiredArgsConstructor
public class CanvasProjectController {

    private final TenantContextResolver tenantContextResolver;
    private final CanvasProjectService projectService;
    private final CanvasProjectPermissionService permissionService;
    private final CanvasService canvasService;
    /**
     * 查询画布项目列表接口，对应 GET 请求。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 projectService.list 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping
    public Mono<R<List<ProjectSummaryResp>>> list() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> R.ok(projectService.list(
                        context.tenantId(),
                        context.username(),
                        context.isSuperAdmin(),
                        context.isTenantAdmin())))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建 画布项目接口，对应 POST 请求。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 projectService.create 完成业务处理。
     * 副作用：会写入新记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体。
     * @return 异步返回统一响应，包含创建 画布项目后的业务数据。
     */
    @PostMapping
    public Mono<R<ProjectDetailResp>> create(@Valid @RequestBody ProjectCreateReq req) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    requireTenantAdmin(context);
                    ProjectCreateReq normalized = new ProjectCreateReq(
                            req.projectKey(),
                            req.projectName(),
                            req.description(),
                            req.defaultSettingsJson(),
                            req.requireReviewBeforePublish(),
                            req.quietHoursJson(),
                            defaultOperator(context, req.operator()));
                    return R.ok(projectService.create(context.tenantId(), normalized));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 获取画布项目详情接口，对应 GET /{projectId}。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 permissionService.requireProjectAction, projectService.detail 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param projectId 项目 ID。
     * @return 异步返回统一响应，包含获取画布项目详情后的业务数据。
     */
    @GetMapping("/{projectId}")
    public Mono<R<ProjectDetailResp>> detail(@PathVariable Long projectId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    permissionService.requireProjectAction(context.tenantId(), projectId, context, CanvasProjectAction.READ);
                    return R.ok(projectService.detail(context.tenantId(), projectId));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 更新 画布项目接口，对应 PUT /{projectId}。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 permissionService.requireProjectAction, projectService.update 完成业务处理。
     * 副作用：会修改已有记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param projectId 项目 ID。
     * @param req 请求体。
     * @return 异步返回统一响应，包含更新 画布项目后的业务数据。
     */
    @PutMapping("/{projectId}")
    public Mono<R<ProjectDetailResp>> update(@PathVariable Long projectId,
                                             @Valid @RequestBody ProjectUpdateReq req) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    permissionService.requireProjectAction(
                            context.tenantId(), projectId, context, CanvasProjectAction.MANAGE_PROJECT);
                    // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                    ProjectUpdateReq normalized = new ProjectUpdateReq(
                            req.projectName(),
                            req.description(),
                            req.defaultSettingsJson(),
                            req.requireReviewBeforePublish(),
                            req.quietHoursJson(),
                            defaultOperator(context, req.operator()));
                    return R.ok(projectService.update(context.tenantId(), projectId, normalized));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 停用 画布项目接口，对应 PUT /{projectId}/disable。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 permissionService.requireProjectAction, projectService.disable 完成业务处理。
     * 副作用：会停用资源。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param projectId 项目 ID。
     * @return 异步返回统一响应，表示操作完成。
     */
    @PutMapping("/{projectId}/disable")
    public Mono<R<Void>> disable(@PathVariable Long projectId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    permissionService.requireProjectAction(
                            context.tenantId(), projectId, context, CanvasProjectAction.MANAGE_PROJECT);
                    projectService.disable(context.tenantId(), projectId, context.username());
                    return R.ok();
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 画布项目 请求接口，对应 GET /{projectId}/members。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 permissionService.requireProjectAction, projectService.listMembers 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param projectId 项目 ID。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/{projectId}/members")
    public Mono<R<List<ProjectMemberResp>>> members(@PathVariable Long projectId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    permissionService.requireProjectAction(context.tenantId(), projectId, context, CanvasProjectAction.READ);
                    return R.ok(projectService.listMembers(context.tenantId(), projectId));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 画布项目 请求接口，对应 PUT /{projectId}/members/{userId}。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 permissionService.requireProjectAction, projectService.setMember 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param projectId 项目 ID。
     * @param userId user ID。
     * @param req 请求体。
     * @return 异步返回统一响应，包含处理 画布项目 请求后的业务数据。
     */
    @PutMapping("/{projectId}/members/{userId}")
    public Mono<R<ProjectMemberResp>> setMember(@PathVariable Long projectId,
                                                @PathVariable Long userId,
                                                @Valid @RequestBody ProjectMemberUpdateReq req) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    permissionService.requireProjectAction(
                            context.tenantId(), projectId, context, CanvasProjectAction.MANAGE_MEMBERS);
                    return R.ok(projectService.setMember(context.tenantId(), projectId, userId, req));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 移除 画布项目 关联接口，对应 DELETE /{projectId}/members/{userId}。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 permissionService.requireProjectAction, projectService.removeMember 完成业务处理。
     * 副作用：会移除关联关系，会变更资源位置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param projectId 项目 ID。
     * @param userId user ID。
     * @return 异步返回统一响应，表示操作完成。
     */
    @DeleteMapping("/{projectId}/members/{userId}")
    public Mono<R<Void>> removeMember(@PathVariable Long projectId,
                                      @PathVariable Long userId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    permissionService.requireProjectAction(
                            context.tenantId(), projectId, context, CanvasProjectAction.MANAGE_MEMBERS);
                    projectService.removeMember(context.tenantId(), projectId, userId);
                    return R.ok();
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 画布项目 请求接口，对应 GET /{projectId}/canvases。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 permissionService.requireProjectAction, canvasService.list 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param projectId 项目 ID。
     * @param page 请求参数，默认值为 1。
     * @param size 请求参数，默认值为 20。
     * @return 异步返回统一响应，包含分页结果。
     */
    @GetMapping("/{projectId}/canvases")
    public Mono<R<PageResult<CanvasDO>>> canvases(@PathVariable Long projectId,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    permissionService.requireProjectAction(context.tenantId(), projectId, context, CanvasProjectAction.READ);
                    CanvasListQuery query = new CanvasListQuery();
                    query.setTenantId(context.tenantId());
                    query.setProjectId(projectId);
                    query.setPage(page);
                    query.setSize(size);
                    return R.ok(canvasService.list(query));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询画布项目统计接口，对应 GET /{projectId}/stats。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 permissionService.requireProjectAction, projectService.stats 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param projectId 项目 ID。
     * @return 异步返回统一响应，包含查询画布项目统计后的业务数据。
     */
    @GetMapping("/{projectId}/stats")
    public Mono<R<ProjectStatsResp>> stats(@PathVariable Long projectId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    permissionService.requireProjectAction(context.tenantId(), projectId, context, CanvasProjectAction.READ);
                    return R.ok(projectService.stats(context.tenantId(), projectId));
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
                .defaultIfEmpty(new TenantContext(0L, null, "system"));
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     */
    private void requireTenantAdmin(TenantContext context) {
        if (!context.isSuperAdmin() && !context.isTenantAdmin()) {
            throw new AccessDeniedException("Tenant admin role is required");
        }
    }

    /**
     * 解析操作人标识。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param requested requested 参数，用于 defaultOperator 流程中的校验、计算或对象转换。
     * @return 返回 default operator 生成的文本或业务键。
     */
    private String defaultOperator(TenantContext context, String requested) {
        return context.username();
    }
}
