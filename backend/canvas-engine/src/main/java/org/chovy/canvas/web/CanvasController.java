package org.chovy.canvas.web;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.canvas.*;
import org.chovy.canvas.domain.compliance.AuditEventService;
import org.chovy.canvas.domain.notification.NotificationEventService;
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

    @Autowired(required = false)
    void setAuditEventService(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
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
                            requireTenantAccess(id, context);
                            canvasService.updateDraft(id, req);
                            recordCanvasAudit(context, context.username(), "canvas update",
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
                            requireTenantAccess(id, context);
                            // 发布会修改版本状态并刷新运行配置，属于阻塞事务操作。
                            CanvasVersionDO version = canvasService.publish(id, operator);
                            notifyCanvasChange("CANVAS_PUBLISHED", id, "画布已发布",
                                    "operator=" + operator + " versionId=" + version.getId(),
                                    "INFO", operator);
                            recordPublishAudit(context, id, operator, version);
                            return version;
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

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
                            requireTenantAccess(id, context);
                            canvasService.offline(id, operator);
                            notifyCanvasChange("CANVAS_OFFLINE", id, "画布已下线",
                                    "operator=" + operator, "WARNING", operator);
                            recordCanvasAudit(context, operator, "canvas offline", id, Map.of());
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok()));
    }

    @PostMapping("/{id}/archive")
    @Operation(operationId = "archiveCanvas", summary = "Archive canvas")
    public Mono<R<Void>> archive(
            @PathVariable Long id,
            @RequestParam(defaultValue = "system") String operator) {
        return currentTenant().flatMap(context ->
                Mono.<Void>fromRunnable(() -> {
                            requireTenantAccess(id, context);
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

    @GetMapping("/{id}/project-folder-metadata")
    @Operation(operationId = "getCanvasProjectFolderMetadata", summary = "Get canvas project folder metadata")
    public Mono<R<ProjectFolderMetadataResp>> getProjectFolderMetadata(@PathVariable Long id) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> {
                            requireTenantAccess(id, context);
                            return projectFolderMetadataService.getMetadata(id);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    @PutMapping("/{id}/project-folder-metadata")
    @Operation(operationId = "saveCanvasProjectFolderMetadata", summary = "Save canvas project folder metadata")
    public Mono<R<ProjectFolderMetadataResp>> saveProjectFolderMetadata(
            @PathVariable Long id,
            @RequestBody ProjectFolderMetadataReq req) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> {
                            requireTenantAccess(id, context);
                            ProjectFolderMetadataReq normalized = new ProjectFolderMetadataReq(
                                    req.projectKey(),
                                    req.projectName(),
                                    req.folderKey(),
                                    req.folderName(),
                                    defaultOperator(req.operator()));
                            return projectFolderMetadataService.saveMetadata(id, normalized);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    /**
     * 执行 current User 对应的业务逻辑。
     *
     * <p>返回值采用 Reactor 异步模型，调用方可继续组合后续处理。
     *
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
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

    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(null, null, null));
    }

    private void applyCreateTenant(CanvasCreateReq req, TenantContext context) {
        if (context.tenantId() == null) {
            return;
        }
        if (!context.isSuperAdmin() || req.getTenantId() == null) {
            req.setTenantId(context.tenantId());
        }
    }

    private void applyQueryTenant(CanvasListQuery query, TenantContext context) {
        if (context.tenantId() == null) {
            return;
        }
        if (!context.isSuperAdmin()) {
            query.setTenantId(context.tenantId());
        }
    }

    private void requireTenantAccess(Long canvasId, TenantContext context) {
        canvasService.requireTenantAccess(canvasId, tenantId(context), isSuperAdmin(context));
    }

    private Long tenantId(TenantContext context) {
        return context == null ? null : context.tenantId();
    }

    private boolean isSuperAdmin(TenantContext context) {
        return context != null && context.isSuperAdmin();
    }

    private String defaultOperator(String operator) {
        return operator == null || operator.isBlank() ? "system" : operator.trim();
    }

    /**
     * 发布或发送 notify Canvas Change 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param type type 类型标识或分类条件
     * @param canvasId canvasId 对应的业务主键或标识
     * @param title title 方法执行所需的业务参数
     * @param content content 方法执行所需的业务参数
     * @param severity severity 方法执行所需的业务参数
     * @param operator operator 操作人标识
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

    private void recordPublishAudit(TenantContext context, Long canvasId, String operator, CanvasVersionDO version) {
        Long versionId = version == null ? null : version.getId();
        recordCanvasAudit(context, operator, "canvas publish", canvasId, metadata("toVersion", versionId), versionId);
    }

    private void recordCanvasAudit(TenantContext context,
                                   String operator,
                                   String operation,
                                   Long canvasId,
                                   Map<String, Object> metadata) {
        recordCanvasAudit(context, operator, operation, canvasId, metadata, null);
    }

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
