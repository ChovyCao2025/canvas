package org.chovy.canvas.web;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.approval.ApprovalInstanceView;
import org.chovy.canvas.domain.approval.CanvasPublishApprovalRequest;
import org.chovy.canvas.domain.approval.CanvasPublishApprovalService;
import org.chovy.canvas.domain.approval.CanvasPublishApprovalStatusView;
import org.chovy.canvas.domain.canvas.*;
import org.chovy.canvas.domain.compliance.AuditEventService;
import org.chovy.canvas.domain.notification.NotificationEventService;
import org.chovy.canvas.domain.project.CanvasProjectAction;
import org.chovy.canvas.domain.project.CanvasProjectPermissionService;
import org.chovy.canvas.dto.*;
import org.chovy.canvas.dto.canvas.CanvasExportPackage;
import org.chovy.canvas.dto.canvas.CanvasImportReq;
import org.chovy.canvas.dto.canvas.CanvasImportResp;
import org.chovy.canvas.dto.canvas.MessagePreviewReq;
import org.chovy.canvas.dto.canvas.MessagePreviewResp;
import org.chovy.canvas.dto.canvas.ProjectFolderMetadataReq;
import org.chovy.canvas.dto.canvas.ProjectFolderMetadataResp;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.LinkedHashMap;
import java.util.Map;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.query.CanvasListQuery;

/**
 * 画布主控制器：
 * 负责画布 CRUD、版本管理、发布下线以及运营管控入口。
 */
@RestController
@RequestMapping("/canvas")
@RequiredArgsConstructor
@Validated
@Tag(name = "Canvas Management", description = "Canvas CRUD, versioning, publish, rollback, import, and export APIs.")
@SecurityRequirement(name = "bearerAuth")
public class CanvasController {

    /** 画布服务，用于处理画布 CRUD 和草稿版本。 */
    private final CanvasService canvasService;
    /** 画布运维服务，用于发布、下线和审批操作。 */
    private final CanvasOpsService opsService;
    /** 通知事件服务，用于发送画布运营通知。 */
    private final NotificationEventService notificationEventService;
    /** 租户上下文解析器，用于给画布 CRUD 注入租户边界。 */
    private final TenantContextResolver tenantContextResolver;
    /** 消息预览服务，只解析 SEND_MESSAGE 节点，不触发发送副作用。 */
    private final CanvasMessagePreviewService messagePreviewService;
    /** 画布导入导出服务，负责版本化包和图配置脱敏。 */
    private final CanvasImportExportService importExportService;
    /** 画布项目/文件夹元数据服务。 */
    private final CanvasProjectFolderMetadataService projectFolderMetadataService;
    /** 发布前检查服务，用于阻断结构性错误。 */
    private final CanvasPrePublishCheckService prePublishCheckService;
    /** 可选审计服务，用于记录运营闭环动作。 */
    private AuditEventService auditEventService;
    /** 可选项目权限服务，用于项目治理上线后的细粒度授权。 */
    private CanvasProjectPermissionService projectPermissionService;
    /** 可选发布审批服务；存在时发布必须先经过审批门禁。 */
    private CanvasPublishApprovalService canvasPublishApprovalService;

    /**
     * 执行 setAuditEventService 流程，围绕 set audit event service 完成校验、计算或结果组装。
     *
     * @param auditEventService 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired(required = false)
    void setAuditEventService(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    /**
     * 执行 setProjectPermissionService 流程，围绕 set project permission service 完成校验、计算或结果组装。
     *
     * @param projectPermissionService 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired(required = false)
    void setProjectPermissionService(CanvasProjectPermissionService projectPermissionService) {
        this.projectPermissionService = projectPermissionService;
    }

    /**
     * 执行 setCanvasPublishApprovalService 流程，围绕 set canvas publish approval service 完成校验、计算或结果组装。
     *
     * @param canvasPublishApprovalService 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired(required = false)
    void setCanvasPublishApprovalService(CanvasPublishApprovalService canvasPublishApprovalService) {
        this.canvasPublishApprovalService = canvasPublishApprovalService;
    }

    /**
     * 创建画布
     *
     * @param req 创建请求对象
     * @return 创建成功的画布信息
     */
    @PostMapping
    @Operation(operationId = "createCanvas", summary = "Create canvas draft")
    public Mono<R<CanvasDO>> create(@Valid @RequestBody CanvasCreateReq req) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> {
                            applyCreateTenant(req, context);
                            requireTargetProjectAction(req.getProjectId(), context, CanvasProjectAction.EDIT);
                            CanvasDO canvas = canvasService.create(req);
                            recordCanvasAudit(context, context.username(), "canvas create",
                                    canvas == null ? null : canvas.getId(),
                                    metadata("name", req.getName()));
                            return canvas;
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    /**
     * 根据 ID 获取画布详情
     *
     * @param id 画布 ID
     * @return 画布详情
     */
    @GetMapping("/{id}")
    @Operation(operationId = "getCanvasById", summary = "Get canvas detail")
    public Mono<R<CanvasDetailDTO>> getById(@PathVariable Long id) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> {
                            CanvasDetailDTO detail = canvasService.getById(id);
                            if (detail == null) {
                                return R.<CanvasDetailDTO>fail("画布不存在");
                            }
                            canvasService.assertTenantAccess(detail.getCanvas(), tenantId(context), isSuperAdmin(context));
                            requireCanvasAction(detail.getCanvas(), context, CanvasProjectAction.READ);
                            return R.ok(detail);
                        })
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 更新画布草稿
     *
     * @param id  画布 ID
     * @param req 更新信息
     * @return 成功响应
     */
    @PutMapping("/{id}")
    @Operation(operationId = "updateCanvasDraft", summary = "Update canvas draft")
    public Mono<R<Void>> update(@PathVariable Long id, @Valid @RequestBody CanvasUpdateReq req) {
        return currentTenant().flatMap(context ->
                Mono.<Void>fromRunnable(() -> {
                            requireCanvasAction(id, context, CanvasProjectAction.EDIT);
                            canvasService.updateDraft(id, req);
                            recordCanvasAudit(context, context.username(), "canvas update",
                                    /**
                                     * 执行 metadata 流程，围绕 metadata 完成校验、计算或结果组装。
                                     *
                                     * @return 返回 metadata 流程生成的业务结果。
                                     */
                                    id, metadata("name", req.getName()));
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok()));
    }

    /**
     * 查询画布列表
     *
     * @param query 查询条件
     * @return 分页后的画布列表
     */
    @GetMapping("/list")
    @Operation(operationId = "listCanvases", summary = "List canvases")
    public Mono<R<PageResult<CanvasDO>>> list(CanvasListQuery query) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> {
                            applyQueryTenant(query, context);
                            return canvasService.list(query);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    /**
     * 发布画布
     *
     * @param id       画布 ID
     * @param operator 操作人标识
     * @return 发布后的版本信息
     */
    @PostMapping("/{id}/publish")
    @Operation(operationId = "publishCanvas", summary = "Publish canvas")
    public Mono<R<CanvasVersionDO>> publish(
            @PathVariable Long id,
            @RequestParam(defaultValue = "system") String operator) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> {
                            requireCanvasAction(id, context, CanvasProjectAction.PUBLISH);
                            // 发布会修改版本状态并刷新运行配置，属于阻塞事务操作。
                            CanvasVersionDO version = canvasPublishApprovalService == null
                                    ? canvasService.publish(id, operator)
                                    : canvasPublishApprovalService.publishWithApprovalGate(tenantId(context), id, operator);
                            notifyCanvasChange("CANVAS_PUBLISHED", id, "画布已发布",
                                    "operator=" + operator + " versionId=" + version.getId(),
                                    "INFO", operator);
                            recordPublishAudit(context, id, operator, version);
                            return version;
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }
    /**
     * 提交画布请求接口（OpenAPI: Submit canvas publish approval）。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 副作用：会提交业务请求。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @param req 请求体，可选。
     * @return 异步返回统一响应，包含提交画布请求后的业务数据。
     */
    @PostMapping("/{id}/submit-review")
    @Operation(operationId = "submitCanvasPublishReview", summary = "Submit canvas publish approval")
    public Mono<R<ApprovalInstanceView>> submitReview(
            @PathVariable Long id,
            @RequestBody(required = false) CanvasPublishApprovalRequest req) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> {
                            requireCanvasAction(id, context, CanvasProjectAction.PUBLISH);
                            return requiredCanvasPublishApprovalService().submitReview(
                                    tenantId(context), id, defaultOperator(context.username()), req);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }
    /**
     * 查询画布状态接口（OpenAPI: Get canvas publish approval status）。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，包含查询画布状态后的业务数据。
     */
    @GetMapping("/{id}/approval-status")
    @Operation(operationId = "getCanvasPublishApprovalStatus", summary = "Get canvas publish approval status")
    public Mono<R<CanvasPublishApprovalStatusView>> approvalStatus(@PathVariable Long id) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> {
                            requireCanvasAction(id, context, CanvasProjectAction.READ);
                            return requiredCanvasPublishApprovalService().approvalStatus(tenantId(context), id);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }
    /**
     * 执行画布发布前检查接口（OpenAPI: Run pre-publish checks）。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 prePublishCheckService.check 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，包含发布 画布后的业务数据。
     */
    @GetMapping("/{id}/pre-publish-checks")
    @Operation(operationId = "prePublishChecks", summary = "Run pre-publish checks")
    public Mono<R<CanvasPrePublishCheckService.Result>> prePublishChecks(@PathVariable Long id) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> {
                            requireTenantAccess(id, context);
                            return prePublishCheckService.check(id);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    /**
     * 下线画布
     *
     * @param id       画布 ID
     * @param operator 操作人标识
     * @return 成功响应
     */
    @PostMapping("/{id}/offline")
    @Operation(operationId = "offlineCanvas", summary = "Offline canvas")
    public Mono<R<Void>> offline(
            @PathVariable Long id,
            @RequestParam(defaultValue = "system") String operator) {
        return currentTenant().flatMap(context ->
                Mono.<Void>fromRunnable(() -> {
                            requireCanvasAction(id, context, CanvasProjectAction.PUBLISH);
                            canvasService.offline(id, operator);
                            notifyCanvasChange("CANVAS_OFFLINE", id, "画布已下线",
                                    "operator=" + operator, "WARNING", operator);
                            recordCanvasAudit(context, operator, "canvas offline", id, Map.of());
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok()));
    }
    /**
     * 归档 画布接口（OpenAPI: Archive canvas）。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 canvasService.archive 完成业务处理。
     * 副作用：会归档资源。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @param operator 操作人标识，默认值为 system。
     * @return 异步返回统一响应，表示操作完成。
     */
    @PostMapping("/{id}/archive")
    @Operation(operationId = "archiveCanvas", summary = "Archive canvas")
    public Mono<R<Void>> archive(
            @PathVariable Long id,
            @RequestParam(defaultValue = "system") String operator) {
        return currentTenant().flatMap(context ->
                Mono.<Void>fromRunnable(() -> {
                            requireCanvasAction(id, context, CanvasProjectAction.PUBLISH);
                            canvasService.archive(id, operator);
                            notifyCanvasChange("CANVAS_ARCHIVED", id, "画布已归档",
                                    "operator=" + operator, "WARNING", operator);
                            recordCanvasAudit(context, operator, "canvas archive", id, Map.of());
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok()));
    }

    /**
     * 获取画布历史版本
     *
     * @param id   画布 ID
     * @param page 页码
     * @param size 每页大小
     * @return 版本列表
     */
    @GetMapping("/{id}/versions")
    @Operation(operationId = "listCanvasVersions", summary = "List canvas versions")
    public Mono<R<PageResult<CanvasVersionDO>>> getVersions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> {
                            requireTenantAccess(id, context);
                            return canvasService.getVersions(id, page, size);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    /**
     * 获取指定版本详情
     *
     * @param id        画布 ID
     * @param versionId 版本 ID
     * @return 版本详情
     */
    @GetMapping("/{id}/versions/{versionId}")
    @Operation(operationId = "getCanvasVersion", summary = "Get canvas version")
    public Mono<R<CanvasVersionDO>> getVersion(
            @PathVariable Long id,
            @PathVariable Long versionId) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> {
                            requireTenantAccess(id, context);
                            CanvasVersionDO version = canvasService.getVersion(id, versionId);
                            return version != null ? R.ok(version) : R.<CanvasVersionDO>fail("版本不存在");
                        })
                        .subscribeOn(Schedulers.boundedElastic()));
    }


    // ── 运营管控 ─────────────────────────────────────────────────

    /**
     * 终止画布执行
     *
     * @param id   画布 ID
     * @param mode 终止模式 (GRACEFUL/FORCE)
     * @return 成功响应
     */
    @PostMapping("/{id}/kill")
    @Operation(operationId = "killCanvasExecution", summary = "Kill canvas execution")
    public Mono<R<Void>> kill(@PathVariable Long id,
                              @RequestParam(defaultValue = "GRACEFUL") String mode) {
        return currentTenant().flatMap(context -> currentUser().flatMap(operator ->
                Mono.<Void>fromRunnable(() -> {
                            requireTenantAccess(id, context);
                            opsService.kill(id, mode);
                            notifyCanvasChange("CANVAS_KILLED", id, "画布执行已终止",
                                    "operator=" + operator + " mode=" + mode, "ERROR", operator);
                            recordCanvasAudit(context, operator, "canvas kill", id, metadata("mode", mode));
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok())));
    }

    /**
     * 回退画布草稿到指定历史版本（不影响已发布线上版本）
     */
    @PostMapping("/{id}/revert/{versionId}")
    @Operation(operationId = "revertCanvasDraftToVersion", summary = "Revert canvas draft to version")
    public Mono<R<Void>> revertToVersion(@PathVariable Long id,
                                         @PathVariable Long versionId) {
        return currentTenant().flatMap(context ->
                Mono.<Void>fromRunnable(() -> {
                            requireTenantAccess(id, context);
                            canvasService.revertToVersion(id, versionId);
                            recordCanvasAudit(context, context.username(), "canvas revert",
                                    /**
                                     * 执行 metadata 流程，围绕 metadata 完成校验、计算或结果组装。
                                     *
                                     * @return 返回 metadata 流程生成的业务结果。
                                     */
                                    id, metadata("toVersion", versionId));
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok()));
    }

    /**
     * 启动画布灰度发布
     *
     * @param id      画布 ID
     * @param percent 灰度流量比例 (0-100)
     * @return 成功响应
     */
    @PostMapping("/{id}/canary")
    @Operation(operationId = "startCanvasCanary", summary = "Start canvas canary")
    public Mono<R<Void>> startCanary(@PathVariable Long id,
                                     @RequestParam int percent) {
        return currentTenant().flatMap(context -> currentUser().flatMap(operator ->
                Mono.<Void>fromRunnable(() -> {
                            requireTenantAccess(id, context);
                            opsService.startCanary(id, percent, operator);
                            notifyCanvasChange("CANVAS_CANARY_STARTED", id, "画布灰度已启动",
                                    "operator=" + operator + " percent=" + percent, "INFO", operator);
                            recordCanvasAudit(context, operator, "canvas canary start",
                                    /**
                                     * 执行 metadata 流程，围绕 metadata 完成校验、计算或结果组装。
                                     *
                                     * @return 返回 metadata 流程生成的业务结果。
                                     */
                                    id, metadata("percent", percent));
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok())));
    }

    /**
     * 将灰度版本晋升为正式版本
     *
     * @param id 画布 ID
     * @return 成功响应
     */
    @PostMapping("/{id}/promote-canary")
    @Operation(operationId = "promoteCanvasCanary", summary = "Promote canvas canary")
    public Mono<R<Void>> promoteCanary(@PathVariable Long id) {
        return currentTenant().flatMap(context -> currentUser().flatMap(operator ->
                Mono.<Void>fromRunnable(() -> {
                            requireTenantAccess(id, context);
                            opsService.promoteCanary(id);
                            notifyCanvasChange("CANVAS_CANARY_PROMOTED", id, "灰度版本已转正",
                                    "operator=" + operator, "SUCCESS", operator);
                            recordCanvasAudit(context, operator, "canvas canary promote", id, Map.of());
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok())));
    }

    /**
     * 回滚灰度发布
     *
     * @param id 画布 ID
     * @return 成功响应
     */
    @PostMapping("/{id}/rollback-canary")
    @Operation(operationId = "rollbackCanvasCanary", summary = "Rollback canvas canary")
    public Mono<R<Void>> rollbackCanary(@PathVariable Long id) {
        return currentTenant().flatMap(context -> currentUser().flatMap(operator ->
                Mono.<Void>fromRunnable(() -> {
                            requireTenantAccess(id, context);
                            opsService.rollbackCanary(id);
                            notifyCanvasChange("CANVAS_CANARY_ROLLED_BACK", id, "灰度发布已回滚",
                                    "operator=" + operator, "WARNING", operator);
                            recordCanvasAudit(context, operator, "canvas canary rollback", id, Map.of());
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok())));
    }

    /**
     * 回滚画布到上一个稳定版本
     *
     * @param id 画布 ID
     * @return 成功响应
     */
    @PostMapping("/{id}/rollback")
    @Operation(operationId = "rollbackCanvas", summary = "Rollback canvas")
    public Mono<R<Void>> rollback(@PathVariable Long id) {
        return currentTenant().flatMap(context -> currentUser().flatMap(operator ->
                Mono.<Void>fromRunnable(() -> {
                            requireTenantAccess(id, context);
                            opsService.rollback(id);
                            notifyCanvasChange("CANVAS_ROLLED_BACK", id, "画布已回滚",
                                    "operator=" + operator, "WARNING", operator);
                            recordCanvasAudit(context, operator, "canvas rollback", id, Map.of());
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok())));
    }

    /**
     * 克隆画布
     *
     * @param id 原画布 ID
     * @return 克隆后的画布信息
     */
    @PostMapping("/{id}/clone")
    @Operation(operationId = "cloneCanvas", summary = "Clone canvas")
    public Mono<R<CanvasDO>> clone(@PathVariable Long id) {
        return currentTenant().flatMap(context -> currentUser().flatMap(operator ->
                Mono.fromCallable(() -> {
                            requireTenantAccess(id, context);
                            CanvasDO canvas = opsService.clone(id, operator);
                            notifyCanvasChange("CANVAS_CLONED", canvas.getId(), "画布已克隆",
                                    "operator=" + operator + " sourceCanvasId=" + id, "INFO", operator);
                            recordCanvasAudit(context, operator, "canvas clone",
                                    canvas.getId(), metadata("sourceCanvasId", id));
                            return canvas;
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok)));
    }

    /**
     * 比较两个版本之间的差异
     *
     * @param id 画布 ID
     * @param v1 版本 ID 1
     * @param v2 版本 ID 2
     * @return 差异对比结果
     */
    @GetMapping("/{id}/versions/{v1}/diff/{v2}")
    @Operation(operationId = "diffCanvasVersions", summary = "Diff canvas versions")
    public Mono<R<Map<String, Object>>> diff(@PathVariable Long id,
                                             @PathVariable Long v1,
                                             @PathVariable Long v2) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> {
                            requireTenantAccess(id, context);
                            return opsService.diff(id, v1, v2);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    /**
     * PUT /canvas/{id} 升级：支持 editVersion 乐观锁
     */
    @PutMapping("/{id}/safe")
    @Operation(operationId = "safeUpdateCanvasDraft", summary = "Update canvas draft with optimistic lock")
    public Mono<R<Void>> safeUpdate(@PathVariable Long id,
                                    @RequestBody SafeUpdateReq req) {

        return currentTenant().flatMap(context -> currentUser().flatMap(operator ->
                Mono.<Void>fromRunnable(() -> {
                            requireTenantAccess(id, context);
                            opsService.saveWithOptimisticLock(
                                    // editVersion 用于乐观锁，前端版本落后时返回可识别的冲突文案。
                                    id, req.getName(), req.getDescription(),
                                    req.getGraphJson(), req.getEditVersion(), operator);
                            recordCanvasAudit(context, operator, "canvas safe update",
                                    /**
                                     * 执行 metadata 流程，围绕 metadata 完成校验、计算或结果组装。
                                     *
                                     * @return 返回 metadata 流程生成的业务结果。
                                     */
                                    id, metadata("editVersion", req.getEditVersion()));
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok())
                        .onErrorResume(e -> {
                            if ("CANVAS_010".equals(e.getMessage()))
                                return Mono.just(R.fail("画布已被他人修改，请刷新后重试"));
                            return Mono.error(e);
                        })));
    }
    /**
     * 预览画布结果接口（OpenAPI: Preview canvas message node）。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 messagePreviewService.preview 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @param req 请求体。
     * @return 异步返回统一响应，包含预览画布结果后的业务数据。
     */
    @PostMapping("/{id}/message-preview")
    @Operation(operationId = "previewCanvasMessage", summary = "Preview canvas message node")
    public Mono<R<MessagePreviewResp>> previewMessage(
            @PathVariable Long id,
            @RequestBody MessagePreviewReq req) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> {
                            requireTenantAccess(id, context);
                            MessagePreviewReq normalized = new MessagePreviewReq(
                                    id, req.nodeId(), req.userId(), req.graphJson(), req.context());
                            return messagePreviewService.preview(normalized);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }
    /**
     * 导出 画布接口（OpenAPI: Export canvas package）。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 importExportService.exportCanvas 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @param versionId version ID。
     * @return 异步返回统一响应，包含导出 画布后的业务数据。
     */
    @GetMapping("/{id}/export")
    @Operation(operationId = "exportCanvas", summary = "Export canvas package")
    public Mono<R<CanvasExportPackage>> exportCanvas(
            @PathVariable Long id,
            @RequestParam Long versionId) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> {
                            requireTenantAccess(id, context);
                            return importExportService.exportCanvas(id, versionId);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }
    /**
     * 导入 画布接口（OpenAPI: Import canvas package）。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 importExportService.importCanvas 完成业务处理。
     * 副作用：会导入并落库资源。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体。
     * @return 异步返回统一响应，包含导入 画布后的业务数据。
     */
    @PostMapping("/import")
    @Operation(operationId = "importCanvas", summary = "Import canvas package")
    public Mono<R<CanvasImportResp>> importCanvas(@RequestBody CanvasImportReq req) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> importExportService.importCanvas(
                                new CanvasImportReq(req.packageJson(), defaultOperator(req.operator())),
                                tenantId(context)))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }
    /**
     * 获取画布详情接口（OpenAPI: Get canvas project folder metadata）。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 projectFolderMetadataService.getMetadata 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，包含获取画布详情后的业务数据。
     */
    @GetMapping("/{id}/project-folder-metadata")
    @Operation(operationId = "getCanvasProjectFolderMetadata", summary = "Get canvas project folder metadata")
    public Mono<R<ProjectFolderMetadataResp>> getProjectFolderMetadata(@PathVariable Long id) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> {
                            requireTenantAccess(id, context);
                            requireCanvasAction(id, context, CanvasProjectAction.READ);
                            return projectFolderMetadataService.getMetadata(tenantId(context), id);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }
    /**
     * 保存画布配置接口（OpenAPI: Save canvas project folder metadata）。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 projectFolderMetadataService.saveMetadata 完成业务处理。
     * 副作用：会保存配置或运行态。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @param req 请求体。
     * @return 异步返回统一响应，包含保存画布配置后的业务数据。
     */
    @PutMapping("/{id}/project-folder-metadata")
    @Operation(operationId = "saveCanvasProjectFolderMetadata", summary = "Save canvas project folder metadata")
    public Mono<R<ProjectFolderMetadataResp>> saveProjectFolderMetadata(
            @PathVariable Long id,
            @RequestBody ProjectFolderMetadataReq req) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> {
                            requireCanvasAction(id, context, CanvasProjectAction.EDIT);
                            requireTargetProjectAction(req.projectId(), context, CanvasProjectAction.EDIT);
                            ProjectFolderMetadataReq normalized = new ProjectFolderMetadataReq(
                                    req.projectId(),
                                    req.projectKey(),
                                    req.projectName(),
                                    req.folderKey(),
                                    req.folderName(),
                                    defaultOperator(req.operator()));
                            return projectFolderMetadataService.saveMetadata(tenantId(context), id, normalized);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

// ── helpers ───────────────────────────────────────────────────

    /** 从安全上下文读取当前用户名，缺失时回退 system。 */
    private Mono<String> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal())
                .cast(io.jsonwebtoken.Claims.class)
                // 管控类操作统一记录 JWT 中的 username 作为审计操作人。
                .map(c -> c.get("username", String.class))
                .defaultIfEmpty("system");
    }

    /**
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 currentTenant 流程生成的业务结果。
     */
    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(null, null, null));
    }

    /**
     * 应用请求中的业务字段或租户约束。
     *
     * @param req 请求对象，承载本次操作的输入参数。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     */
    private void applyCreateTenant(CanvasCreateReq req, TenantContext context) {
        if (context.tenantId() == null) {
            return;
        }
        if (!context.isSuperAdmin() || req.getTenantId() == null) {
            req.setTenantId(context.tenantId());
        }
    }

    /**
     * 应用请求中的业务字段或租户约束。
     *
     * @param query query 参数，用于 applyQueryTenant 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     */
    private void applyQueryTenant(CanvasListQuery query, TenantContext context) {
        if (context.tenantId() == null) {
            return;
        }
        if (!context.isSuperAdmin()) {
            query.setTenantId(context.tenantId());
        }
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     */
    private void requireTenantAccess(Long canvasId, TenantContext context) {
        canvasService.requireTenantAccess(canvasId, tenantId(context), isSuperAdmin(context));
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param action action 参数，用于 requireCanvasAction 流程中的校验、计算或对象转换。
     * @return 返回 requireCanvasAction 流程生成的业务结果。
     */
    private CanvasDO requireCanvasAction(Long canvasId, TenantContext context, CanvasProjectAction action) {
        CanvasDO canvas = canvasService.requireTenantAccess(canvasId, tenantId(context), isSuperAdmin(context));
        requireCanvasAction(canvas, context, action);
        return canvas;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param canvas canvas 参数，用于 requireCanvasAction 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param action action 参数，用于 requireCanvasAction 流程中的校验、计算或对象转换。
     */
    private void requireCanvasAction(CanvasDO canvas, TenantContext context, CanvasProjectAction action) {
        if (projectPermissionService != null) {
            projectPermissionService.requireCanvasAction(canvas, context, action);
        }
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param projectId 业务对象 ID，用于定位具体记录。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param action action 参数，用于 requireTargetProjectAction 流程中的校验、计算或对象转换。
     */
    private void requireTargetProjectAction(Long projectId, TenantContext context, CanvasProjectAction action) {
        if (projectId != null && projectPermissionService != null) {
            projectPermissionService.requireProjectAction(tenantId(context), projectId, context, action);
        }
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private Long tenantId(TenantContext context) {
        return context == null ? null : context.tenantId();
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回布尔判断结果。
     */
    private boolean isSuperAdmin(TenantContext context) {
        return context != null && context.isSuperAdmin();
    }

    /**
     * 解析操作人标识。
     *
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 default operator 生成的文本或业务键。
     */
    private String defaultOperator(String operator) {
        return operator == null || operator.isBlank() ? "system" : operator.trim();
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @return 返回 requiredCanvasPublishApprovalService 流程生成的业务结果。
     */
    private CanvasPublishApprovalService requiredCanvasPublishApprovalService() {
        if (canvasPublishApprovalService == null) {
            throw new IllegalStateException("Canvas publish approval service is not configured");
        }
        return canvasPublishApprovalService;
    }

    /**
     * 执行 notifyCanvasChange 流程，围绕 notify canvas change 完成校验、计算或结果组装。
     *
     * @param type 类型标识，用于选择对应处理分支。
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param title title 参数，用于 notifyCanvasChange 流程中的校验、计算或对象转换。
     * @param content content 参数，用于 notifyCanvasChange 流程中的校验、计算或对象转换。
     * @param severity severity 参数，用于 notifyCanvasChange 流程中的校验、计算或对象转换。
     * @param operator 操作人标识，用于审计和权限判断。
     */
    private void notifyCanvasChange(
            String type,
            Long canvasId,
            String title,
            String content,
            String severity,
            String operator) {
        // 控制器内只发领域事件，实际通知落库/推送由通知服务处理。
        notificationEventService.canvasChanged(type, canvasId, title, content, severity, operator);
    }

    /**
     * 记录审计、指标或状态变更信息。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param version version 参数，用于 recordPublishAudit 流程中的校验、计算或对象转换。
     */
    private void recordPublishAudit(TenantContext context, Long canvasId, String operator, CanvasVersionDO version) {
        Long versionId = version == null ? null : version.getId();
        recordCanvasAudit(context, operator, "canvas publish", canvasId, metadata("toVersion", versionId), versionId);
    }

    /**
     * 记录审计、指标或状态变更信息。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param operation 待调度任务或操作名称，用于封装阻塞工作。
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param metadata metadata 参数，用于 recordCanvasAudit 流程中的校验、计算或对象转换。
     */
    private void recordCanvasAudit(TenantContext context,
                                   String operator,
                                   String operation,
                                   Long canvasId,
                                   Map<String, Object> metadata) {
        recordCanvasAudit(context, operator, operation, canvasId, metadata, null);
    }

    /**
     * 记录审计、指标或状态变更信息。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param operation 待调度任务或操作名称，用于封装阻塞工作。
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param metadata metadata 参数，用于 recordCanvasAudit 流程中的校验、计算或对象转换。
     * @param toVersion to version 参数，用于 recordCanvasAudit 流程中的校验、计算或对象转换。
     */
    private void recordCanvasAudit(TenantContext context,
                                   String operator,
                                   String operation,
                                   Long canvasId,
                                   Map<String, Object> metadata,
                                   Long toVersion) {
        if (auditEventService == null) {
            return;
        }
        auditEventService.record(AuditEventService.AuditEventCommand.builder()
                .tenantId(tenantId(context))
                .actor(defaultOperator(operator))
                .actorRole(context == null ? null : context.role())
                .operation(operation)
                .targetType("canvas")
                .targetId(canvasId == null ? "0" : String.valueOf(canvasId))
                .toVersion(toVersion)
                .metadata(metadata == null ? Map.of() : metadata)
                .build());
    }

    /**
     * 执行 metadata 流程，围绕 metadata 完成校验、计算或结果组装。
     *
     * @param keyValues key values 参数，用于 metadata 流程中的校验、计算或对象转换。
     * @return 返回 metadata 流程生成的业务结果。
     */
    private Map<String, Object> metadata(Object... keyValues) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            if (keyValues[i] != null && keyValues[i + 1] != null) {
                metadata.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
            }
        }
        return metadata;
    }

}
