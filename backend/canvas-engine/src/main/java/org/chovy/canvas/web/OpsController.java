package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.canvas.*;
import org.chovy.canvas.common.enums.ApprovalStatus;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.VersionStatus;
import org.chovy.canvas.domain.notification.NotificationEventService;
import org.chovy.canvas.domain.ops.OpsAuditEventService;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.chovy.canvas.infrastructure.redis.TriggerRouteRecoveryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.List;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasManualApprovalDO;
import org.chovy.canvas.dal.mapper.CanvasManualApprovalMapper;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.dataobject.CanvasTemplateDO;
import org.chovy.canvas.dal.mapper.CanvasTemplateMapper;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;

/**
 * 运营工具 API（设计文档第二十三章）
 */
@RestController
@RequiredArgsConstructor
public class OpsController {

    /** 画布模板 Mapper，用于管理模板记录。 */
    private final CanvasTemplateMapper templateMapper;
    /** 画布 Mapper，用于读取和创建画布记录。 */
    private final CanvasMapper canvasMapper;
    /** 画布版本 Mapper，用于读取模板对应版本。 */
    private final CanvasVersionMapper canvasVersionMapper;
    /** 人工审批 Mapper，用于查询审批记录。 */
    private final CanvasManualApprovalMapper approvalMapper;
    /** 画布配置缓存，用于刷新画布配置缓存。 */
    private final CanvasConfigCache configCache;
    /** Redis 路由和本机调度恢复服务。 */
    private final TriggerRouteRecoveryService routeRecoveryService;
    /** 当前登录租户上下文解析器。 */
    private final TenantContextResolver tenantContextResolver;
    /** 画布生命周期服务。 */
    private final CanvasService canvasService;
    /** 画布运营控制服务。 */
    private final CanvasOpsService canvasOpsService;
    /** 运维审计事件服务。 */
    private final OpsAuditEventService opsAuditEventService;
    /** 通知事件服务。 */
    private final NotificationEventService notificationEventService;

    public OpsController(CanvasTemplateMapper templateMapper,
                         CanvasMapper canvasMapper,
                         CanvasVersionMapper canvasVersionMapper,
                         CanvasManualApprovalMapper approvalMapper,
                         CanvasConfigCache configCache,
                         TriggerRouteRecoveryService routeRecoveryService) {
        this(templateMapper, canvasMapper, canvasVersionMapper, approvalMapper, configCache,
                routeRecoveryService, null, null, null, null, null);
    }

    /**
     * 处理 invalidate Cache 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param id id 对应的业务主键或标识
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
// ── 缓存管理 ─────────────────────────────────────────────────────

    /**
     * 强制失效指定画布的配置缓存（L1 Caffeine + L2 Redis）。
     * 用于 Flyway 数据迁移后、或缓存脏数据时手动刷新。
     *
     * @param id 画布 ID
     */
    @PostMapping("/ops/cache/invalidate/{id}")
    public Mono<R<String>> invalidateCache(@PathVariable Long id) {
        return Mono.fromCallable(() -> {
            CanvasDO canvas = canvasMapper.selectById(id);
            if (canvas == null) return R.<String>fail("画布不存在: " + id);
            if (canvas.getPublishedVersionId() != null) {
                configCache.invalidate(id, canvas.getPublishedVersionId());
            }
            if (canvas.getCanaryVersionId() != null) {
                configCache.invalidate(id, canvas.getCanaryVersionId());
            }
            return R.ok("已失效画布 " + id + " 的缓存");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 从已发布画布版本重建 Redis 触发路由和本机定时调度注册。 */
    @PostMapping("/ops/recovery/runtime-state/rebuild")
    public Mono<R<TriggerRouteRecoveryService.RecoveryReport>> rebuildRuntimeState() {
        return Mono.fromCallable(() -> R.ok(routeRecoveryService.rebuildRuntimeState()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/ops/runtime/status")
    public Mono<R<RuntimeStatus>> runtimeStatus() {
        return requiredTenantContext().map(context -> R.ok(new RuntimeStatus(
                "UP",
                context.role(),
                context.tenantId(),
                context.username())));
    }

    @GetMapping("/ops/audit-events")
    public Mono<R<List<OpsAuditEventService.OpsAuditEvent>>> auditEvents(
            @RequestParam(defaultValue = "50") int limit) {
        return requiredTenantContext().map(context -> R.ok(
                requiredAuditService().recent(context.isSuperAdmin() ? null : context.tenantId(), limit)));
    }

    @PostMapping("/ops/canvas/{id}/pause")
    public Mono<R<Map<String, Object>>> pauseCanvas(
            @PathVariable Long id,
            @RequestBody EmergencyActionReq req) {
        return emergencyAction(id, "PAUSE", req,
                (context, canvas) -> requiredCanvasService().offline(id, operator(context)));
    }

    @PostMapping("/ops/canvas/{id}/offline")
    public Mono<R<Map<String, Object>>> offlineCanvas(
            @PathVariable Long id,
            @RequestBody EmergencyActionReq req) {
        return emergencyAction(id, "OFFLINE", req,
                (context, canvas) -> requiredCanvasService().offline(id, operator(context)));
    }

    @PostMapping("/ops/canvas/{id}/resume")
    public Mono<R<Map<String, Object>>> resumeCanvas(
            @PathVariable Long id,
            @RequestBody EmergencyActionReq req) {
        return emergencyAction(id, "RESUME", req,
                (context, canvas) -> requiredCanvasService().publish(id, operator(context)));
    }

    @PostMapping("/ops/canvas/{id}/kill")
    public Mono<R<Map<String, Object>>> killCanvas(
            @PathVariable Long id,
            @RequestBody EmergencyActionReq req) {
        return emergencyAction(id, "KILL", req,
                (context, canvas) -> requiredCanvasOpsService().kill(id, defaultIfBlank(req.getMode(), "GRACEFUL")));
    }

    @PostMapping("/ops/canvas/{id}/rollback")
    public Mono<R<Map<String, Object>>> rollbackCanvas(
            @PathVariable Long id,
            @RequestBody EmergencyActionReq req) {
        return emergencyAction(id, "ROLLBACK", req,
                (context, canvas) -> requiredCanvasOpsService().rollback(id));
    }

    // ── 画布模板（23.1节） ─────────────────────────────────────────

    /**
     * 获取画布模板列表
     *
     * @param category 模板分类（可选）
     * @return 模板列表
     */
    @GetMapping("/canvas/templates")
    public Mono<R<List<CanvasTemplateDO>>> listTemplates(
            @RequestParam(required = false) String category) {
        return Mono.fromCallable(() -> {
            QueryWrapper<CanvasTemplateDO> q = new QueryWrapper<CanvasTemplateDO>()
                    .eq("enabled", 1)
                    .orderByDesc("use_count");
            if (category != null && !category.isBlank()) {
                q.eq("category", category);
            }
            return templateMapper.selectList(q);
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    /**
     * 将当前画布另存为模板
     *
     * @param id  画布 ID
     * @param req 模板信息（名称、分类等）
     * @return 模板对象
     */
    @PostMapping("/canvas/{id}/save-as-template")
    public Mono<R<CanvasTemplateDO>> saveAsTemplate(
            @PathVariable Long id,
            @RequestBody SaveTemplateReq req) {
        return Mono.fromCallable(() -> {
            CanvasDO canvas = canvasMapper.selectById(id);
            if (canvas == null) throw new IllegalArgumentException("画布不存在");
            CanvasTemplateDO tpl = new CanvasTemplateDO();
            tpl.setName(req.getName() != null ? req.getName() : canvas.getName() + " 模板");
            tpl.setDescription(req.getDescription());
            tpl.setCategory(req.getCategory());
            // 获取最新草稿的 graphJson
            var draft = canvasVersionMapper.selectOne(
                    new LambdaQueryWrapper<CanvasVersionDO>()
                            .eq(CanvasVersionDO::getCanvasId, id)
                            .eq(CanvasVersionDO::getStatus, 0)
                            .orderByDesc(CanvasVersionDO::getVersion).last("LIMIT 1"));
            tpl.setGraphJson(draft != null ? draft.getGraphJson() : "{\"nodes\":[]}");
            tpl.setIsOfficial(0);
            tpl.setEnabled(1);
            tpl.setUseCount(0);
            tpl.setCreatedBy("current_user");
            templateMapper.insert(tpl);
            return tpl;
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    /**
     * 基于模板创建新画布
     *
     * @param templateId 模板 ID
     * @param req        画布名称
     * @return 新画布信息
     */
    @PostMapping("/canvas/from-template/{templateId}")
    public Mono<R<CanvasDO>> createFromTemplate(@PathVariable Long templateId,
                                              @RequestBody FromTemplateReq req) {
        return Mono.fromCallable(() -> {
            CanvasTemplateDO tpl = templateMapper.selectById(templateId);
            if (tpl == null) throw new IllegalArgumentException("模板不存在");

            CanvasDO canvas = new CanvasDO();
            canvas.setName(req.getName() != null ? req.getName() : tpl.getName() + " (副本)");
            canvas.setDescription(tpl.getDescription());
            canvas.setStatus(CanvasStatusEnum.DRAFT.getCode());
            canvas.setCreatedBy("current_user");
            canvas.setIsExample(0);
            canvas.setSourceTemplateKey(null);
            canvasMapper.insert(canvas);

            CanvasVersionDO version = new CanvasVersionDO();
            version.setCanvasId(canvas.getId());
            version.setVersion(1);
            version.setGraphJson(tpl.getGraphJson());
            version.setStatus(VersionStatus.DRAFT.getCode());
            version.setCreatedBy("current_user");
            canvasVersionMapper.insert(version);

            // 更新模板使用次数
            tpl.setUseCount(tpl.getUseCount() == null ? 1 : tpl.getUseCount() + 1);
            templateMapper.updateById(tpl);

            return canvas;
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

// ── 发布审批（23.2节） ─────────────────────────────────────────

    /**
     * 获取待审批的发布请求列表
     *
     * @return 审批记录列表
     */
    @GetMapping("/canvas/pending-reviews")
    public Mono<R<List<CanvasManualApprovalDO>>> pendingReviews() {
        return Mono.fromCallable(() ->
                approvalMapper.selectList(
                        new LambdaQueryWrapper<CanvasManualApprovalDO>()
                                .eq(CanvasManualApprovalDO::getStatus, ApprovalStatus.PENDING)
                                .orderByAsc(CanvasManualApprovalDO::getTimeoutAt))
        ).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }


    // ── DTOs ──────────────────────────────────────────────────────

    /**
     * 保存模板请求体。
     */
    @Data
    static class SaveTemplateReq {

        /** 模板名称。 */
        private String name;

        /** 模板描述。 */
        private String description;

        /** 模板分类。 */
        private String category;
    }

    /**
     * 从模板创建画布请求体。
     */
    @Data
    static class FromTemplateReq {

        /** 新建画布名称（可选，不传则使用模板名 + 副本后缀）。 */
        private String name;
    }

    @Data
    public static class EmergencyActionReq {
        private String reason;
        private String mode;
    }

    public record RuntimeStatus(
            String status,
            String role,
            Long tenantId,
            String username) {
    }

    @FunctionalInterface
    private interface EmergencyOperation {
        void execute(TenantContext context, CanvasDO canvas);
    }

    private Mono<R<Map<String, Object>>> emergencyAction(
            Long canvasId,
            String action,
            EmergencyActionReq req,
            EmergencyOperation operation) {
        return requiredTenantContext()
                .flatMap(context -> Mono.fromCallable(() -> {
                    requireEmergencyPermission(context);
                    String reason = requireReason(req);
                    CanvasDO canvas = requiredCanvasService().requireTenantAccess(
                            canvasId,
                            context.isSuperAdmin() ? null : context.tenantId(),
                            context.isSuperAdmin());
                    operation.execute(context, canvas);
                    OpsAuditEventService.OpsAuditEvent audit = requiredAuditService().record(
                            canvas.getTenantId(),
                            action,
                            canvasId,
                            operator(context),
                            context.role(),
                            reason);
                    if (notificationEventService != null) {
                        notificationEventService.emergencyActionCompleted(action, canvasId, operator(context), reason);
                    }
                    Map<String, Object> result = Map.of(
                            "action", action,
                            "canvasId", canvasId,
                            "auditId", audit.id());
                    return R.ok(result);
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<TenantContext> requiredTenantContext() {
        if (tenantContextResolver == null) {
            return Mono.error(new SecurityException("AUTH_003: missing tenant context"));
        }
        return tenantContextResolver.currentOrError();
    }

    private void requireEmergencyPermission(TenantContext context) {
        if (context == null || (!context.isSuperAdmin() && !context.isTenantAdmin())) {
            throw new AccessDeniedException("无权限执行运维应急动作");
        }
    }

    private String requireReason(EmergencyActionReq req) {
        String reason = req == null ? null : req.getReason();
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
        return reason.trim();
    }

    private String operator(TenantContext context) {
        return context != null && context.username() != null && !context.username().isBlank()
                ? context.username()
                : "operator";
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private CanvasService requiredCanvasService() {
        if (canvasService == null) {
            throw new IllegalStateException("canvasService is not configured");
        }
        return canvasService;
    }

    private CanvasOpsService requiredCanvasOpsService() {
        if (canvasOpsService == null) {
            throw new IllegalStateException("canvasOpsService is not configured");
        }
        return canvasOpsService;
    }

    private OpsAuditEventService requiredAuditService() {
        if (opsAuditEventService == null) {
            throw new IllegalStateException("opsAuditEventService is not configured");
        }
        return opsAuditEventService;
    }
}
