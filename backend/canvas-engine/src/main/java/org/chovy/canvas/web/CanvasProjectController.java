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

@RestController
@RequestMapping("/admin/projects")
@RequiredArgsConstructor
public class CanvasProjectController {

    private final TenantContextResolver tenantContextResolver;
    private final CanvasProjectService projectService;
    private final CanvasProjectPermissionService permissionService;
    private final CanvasService canvasService;

    @GetMapping
    public Mono<R<List<ProjectSummaryResp>>> list() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> R.ok(projectService.list(
                        context.tenantId(),
                        context.username(),
                        context.isSuperAdmin(),
                        context.isTenantAdmin())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

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

    @GetMapping("/{projectId}")
    public Mono<R<ProjectDetailResp>> detail(@PathVariable Long projectId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    permissionService.requireProjectAction(context.tenantId(), projectId, context, CanvasProjectAction.READ);
                    return R.ok(projectService.detail(context.tenantId(), projectId));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PutMapping("/{projectId}")
    public Mono<R<ProjectDetailResp>> update(@PathVariable Long projectId,
                                             @Valid @RequestBody ProjectUpdateReq req) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    permissionService.requireProjectAction(
                            context.tenantId(), projectId, context, CanvasProjectAction.MANAGE_PROJECT);
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

    @GetMapping("/{projectId}/members")
    public Mono<R<List<ProjectMemberResp>>> members(@PathVariable Long projectId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    permissionService.requireProjectAction(context.tenantId(), projectId, context, CanvasProjectAction.READ);
                    return R.ok(projectService.listMembers(context.tenantId(), projectId));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

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

    @GetMapping("/{projectId}/stats")
    public Mono<R<ProjectStatsResp>> stats(@PathVariable Long projectId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    permissionService.requireProjectAction(context.tenantId(), projectId, context, CanvasProjectAction.READ);
                    return R.ok(projectService.stats(context.tenantId(), projectId));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(0L, null, "system"));
    }

    private void requireTenantAdmin(TenantContext context) {
        if (!context.isSuperAdmin() && !context.isTenantAdmin()) {
            throw new AccessDeniedException("Tenant admin role is required");
        }
    }

    private String defaultOperator(TenantContext context, String requested) {
        return context.username();
    }
}
