package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.canvas.*;
import org.chovy.canvas.common.enums.ApprovalStatus;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.VersionStatus;
import org.chovy.canvas.domain.approval.ApprovalTaskView;
import org.chovy.canvas.domain.approval.ApprovalWorkflowService;
import org.chovy.canvas.domain.notification.NotificationEventService;
import org.chovy.canvas.domain.ops.OpsAuditEventService;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.chovy.canvas.infrastructure.redis.TriggerRouteRecoveryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
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
    /** 统一审批工作流服务；存在时 pending-reviews 改读统一审批任务。 */
    private ApprovalWorkflowService approvalWorkflowService;

    /**
     * 创建 OpsController 实例并注入 web 场景依赖。
     * @param templateMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param canvasMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param canvasVersionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param approvalMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param configCache 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param routeRecoveryService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param canvasService 依赖组件，用于完成数据访问或外部能力调用。
     * @param canvasOpsService 依赖组件，用于完成数据访问或外部能力调用。
     * @param opsAuditEventService 依赖组件，用于完成数据访问或外部能力调用。
     * @param notificationEventService 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public OpsController(CanvasTemplateMapper templateMapper,
                         CanvasMapper canvasMapper,
                         CanvasVersionMapper canvasVersionMapper,
                         CanvasManualApprovalMapper approvalMapper,
                         CanvasConfigCache configCache,
                         TriggerRouteRecoveryService routeRecoveryService,
                         TenantContextResolver tenantContextResolver,
                         CanvasService canvasService,
                         CanvasOpsService canvasOpsService,
                         OpsAuditEventService opsAuditEventService,
                         NotificationEventService notificationEventService) {
        this.templateMapper = templateMapper;
        this.canvasMapper = canvasMapper;
        this.canvasVersionMapper = canvasVersionMapper;
        this.approvalMapper = approvalMapper;
        this.configCache = configCache;
        this.routeRecoveryService = routeRecoveryService;
        this.tenantContextResolver = tenantContextResolver;
        this.canvasService = canvasService;
        this.canvasOpsService = canvasOpsService;
        this.opsAuditEventService = opsAuditEventService;
        this.notificationEventService = notificationEventService;
    }

    /**
     * 创建 OpsController 实例并注入 web 场景依赖。
     * @param templateMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param canvasMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param canvasVersionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param approvalMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param configCache 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param routeRecoveryService 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired(required = false)
    void setApprovalWorkflowService(ApprovalWorkflowService approvalWorkflowService) {
        this.approvalWorkflowService = approvalWorkflowService;
    }

    /**
     * 执行 OpsController 流程，围绕 ops controller 完成校验、计算或结果组装。
     *
     * @param templateMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param canvasMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param canvasVersionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param approvalMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param configCache 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param routeRecoveryService 依赖组件，用于完成数据访问或外部能力调用。
     */
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
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            CanvasDO canvas = canvasMapper.selectById(id);
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (canvas == null) return R.<String>fail("画布不存在: " + id);
            if (canvas.getPublishedVersionId() != null) {
                configCache.invalidate(id, canvas.getPublishedVersionId());
            }
            if (canvas.getCanaryVersionId() != null) {
                configCache.invalidate(id, canvas.getCanaryVersionId());
            }
            // 汇总前面计算出的状态和明细，返回给调用方。
            return R.ok("已失效画布 " + id + " 的缓存");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 从已发布画布版本重建 Redis 触发路由和本机定时调度注册。 */
    @PostMapping("/ops/recovery/runtime-state/rebuild")
    public Mono<R<TriggerRouteRecoveryService.RecoveryReport>> rebuildRuntimeState() {
        return Mono.fromCallable(() -> R.ok(routeRecoveryService.rebuildRuntimeState()))
                .subscribeOn(Schedulers.boundedElastic());
    }
    /**
     * 触发运维运行接口，对应 GET /ops/runtime/status。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 该接口只读取数据，不主动触发业务写入。
     * 方法以 Mono 作为异步边界返回，实际执行由 Reactor 链路承载。
     *
     * @return 异步返回统一响应，包含触发运维运行后的业务数据。
     */
    @GetMapping("/ops/runtime/status")
    public Mono<R<RuntimeStatus>> runtimeStatus() {
        return requiredTenantContext().map(context -> R.ok(new RuntimeStatus(
                "UP",
                context.role(),
                context.tenantId(),
                context.username())));
    }
    /**
     * 查询运维审计接口，对应 GET /ops/audit-events。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 方法以 Mono 作为异步边界返回，实际执行由 Reactor 链路承载。
     *
     * @param limit 返回数量上限，默认值为 50。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/ops/audit-events")
    public Mono<R<List<OpsAuditEventService.OpsAuditEvent>>> auditEvents(
            @RequestParam(defaultValue = "50") int limit) {
        return requiredTenantContext().map(context -> R.ok(
                requiredAuditService().recent(context.isSuperAdmin() ? null : context.tenantId(), limit)));
    }
    /**
     * 暂停 运维接口，对应 POST /ops/canvas/{id}/pause。
     * 接口在控制器或服务层执行资源权限校验后再处理请求。
     * 副作用：会暂停资源。
     * 方法以 Mono 作为异步边界返回，实际执行由 Reactor 链路承载。
     *
     * @param id 资源 ID。
     * @param req 请求体。
     * @return 异步返回统一响应，包含键值结果。
     */
    @PostMapping("/ops/canvas/{id}/pause")
    public Mono<R<Map<String, Object>>> pauseCanvas(
            @PathVariable Long id,
            @RequestBody EmergencyActionReq req) {
        return emergencyAction(id, "PAUSE", req,
                (context, canvas) -> requiredCanvasService().offline(id, operator(context)));
    }
    /**
     * 处理 运维 请求接口，对应 POST /ops/canvas/{id}/offline。
     * 接口在控制器或服务层执行资源权限校验后再处理请求。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 方法以 Mono 作为异步边界返回，实际执行由 Reactor 链路承载。
     *
     * @param id 资源 ID。
     * @param req 请求体。
     * @return 异步返回统一响应，包含键值结果。
     */
    @PostMapping("/ops/canvas/{id}/offline")
    public Mono<R<Map<String, Object>>> offlineCanvas(
            @PathVariable Long id,
            @RequestBody EmergencyActionReq req) {
        return emergencyAction(id, "OFFLINE", req,
                (context, canvas) -> requiredCanvasService().offline(id, operator(context)));
    }
    /**
     * 恢复 运维接口，对应 POST /ops/canvas/{id}/resume。
     * 接口在控制器或服务层执行资源权限校验后再处理请求。
     * 副作用：会恢复资源。
     * 方法以 Mono 作为异步边界返回，实际执行由 Reactor 链路承载。
     *
     * @param id 资源 ID。
     * @param req 请求体。
     * @return 异步返回统一响应，包含键值结果。
     */
    @PostMapping("/ops/canvas/{id}/resume")
    public Mono<R<Map<String, Object>>> resumeCanvas(
            @PathVariable Long id,
            @RequestBody EmergencyActionReq req) {
        return emergencyAction(id, "RESUME", req,
                (context, canvas) -> requiredCanvasService().publish(id, operator(context)));
    }
    /**
     * 处理 运维 请求接口，对应 POST /ops/canvas/{id}/kill。
     * 接口在控制器或服务层执行资源权限校验后再处理请求。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 方法以 Mono 作为异步边界返回，实际执行由 Reactor 链路承载。
     *
     * @param id 资源 ID。
     * @param req 请求体。
     * @return 异步返回统一响应，包含键值结果。
     */
    @PostMapping("/ops/canvas/{id}/kill")
    public Mono<R<Map<String, Object>>> killCanvas(
            @PathVariable Long id,
            @RequestBody EmergencyActionReq req) {
        return emergencyAction(id, "KILL", req,
                (context, canvas) -> requiredCanvasOpsService().kill(id, defaultIfBlank(req.getMode(), "GRACEFUL")));
    }
    /**
     * 回滚运维操作接口，对应 POST /ops/canvas/{id}/rollback。
     * 接口在控制器或服务层执行资源权限校验后再处理请求。
     * 副作用：会执行回滚动作。
     * 方法以 Mono 作为异步边界返回，实际执行由 Reactor 链路承载。
     *
     * @param id 资源 ID。
     * @param req 请求体。
     * @return 异步返回统一响应，包含键值结果。
     */
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
    public Mono<R<List<ApprovalTaskView>>> pendingReviews() {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (approvalWorkflowService != null) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            return requiredTenantContext().flatMap(context ->
                    Mono.fromCallable(() -> R.ok(approvalWorkflowService.listTasks(
                                    context.tenantId() == null ? 0L : context.tenantId(),
                                    operator(context),
                                    context.role(),
                                    ApprovalWorkflowService.STATUS_PENDING)))
                            .subscribeOn(Schedulers.boundedElastic()));
        }
        return Mono.fromCallable(() ->
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                approvalMapper.selectList(
                                new LambdaQueryWrapper<CanvasManualApprovalDO>()
                                        .eq(CanvasManualApprovalDO::getStatus, ApprovalStatus.PENDING)
                                        .orderByAsc(CanvasManualApprovalDO::getTimeoutAt))
                        .stream()
                        .map(this::legacyApprovalTask)
                        .toList()
        ).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    /**
     * 执行 legacyApprovalTask 流程，围绕 legacy approval task 完成校验、计算或结果组装。
     *
     * @param approval approval 参数，用于 legacyApprovalTask 流程中的校验、计算或对象转换。
     * @return 返回 legacyApprovalTask 流程生成的业务结果。
     */
    private ApprovalTaskView legacyApprovalTask(CanvasManualApprovalDO approval) {
        return new ApprovalTaskView(
                null,
                null,
                null,
                1,
                approval.getApprovers(),
                approval.getStatus(),
                null,
                approval.getTimeoutAt(),
                approval.getResultAt(),
                null);
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
    /**
     * EmergencyActionReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class EmergencyActionReq {
        /** 发起变更、控制或回滚的原因，用于审批和审计追溯。 */
        private String reason;
        /** 执行模式，用于区分自动判定、人工触发或混合巡检流程。 */
        private String mode;
    }

    /**
     * RuntimeStatus 创建或触发 web 场景的业务处理。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @return 返回 RuntimeStatus 流程生成的业务结果。
     */
    public record RuntimeStatus(
            String status,
            String role,
            Long tenantId,
            String username) {
    }

    /**
     * EmergencyOperation 接口契约。
     */
    @FunctionalInterface
    private interface EmergencyOperation {
        /**
         * 执行核心业务处理流程。
         *
         * @param context 上下文对象，承载租户、身份或运行时信息。
         * @param canvas canvas 参数，用于 execute 流程中的校验、计算或对象转换。
         */
        void execute(TenantContext context, CanvasDO canvas);
    }

    /**
     * 执行 emergencyAction 流程，围绕 emergency action 完成校验、计算或结果组装。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param action action 参数，用于 emergencyAction 流程中的校验、计算或对象转换。
     * @param req 请求对象，承载本次操作的输入参数。
     * @param operation 待调度任务或操作名称，用于封装阻塞工作。
     * @return 返回 emergencyAction 流程生成的业务结果。
     */
    private Mono<R<Map<String, Object>>> emergencyAction(
            Long canvasId,
            String action,
            EmergencyActionReq req,
            EmergencyOperation operation) {
        return requiredTenantContext()
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .flatMap(context -> Mono.fromCallable(() -> {
                    requireEmergencyPermission(context);
                    String reason = requireReason(req);
                    CanvasDO canvas = requiredCanvasService().requireTenantAccess(
                            canvasId,
                            context.isSuperAdmin() ? null : context.tenantId(),
                            context.isSuperAdmin());
                    // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
                    // 汇总前面计算出的状态和明细，返回给调用方。
                    return R.ok(result);
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @return 返回 requiredTenantContext 流程生成的业务结果。
     */
    private Mono<TenantContext> requiredTenantContext() {
        if (tenantContextResolver == null) {
            return Mono.error(new SecurityException("AUTH_003: missing tenant context"));
        }
        return tenantContextResolver.currentOrError();
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     */
    private void requireEmergencyPermission(TenantContext context) {
        if (context == null || (!context.isSuperAdmin() && !context.isTenantAdmin())) {
            throw new AccessDeniedException("无权限执行运维应急动作");
        }
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回 require reason 生成的文本或业务键。
     */
    private String requireReason(EmergencyActionReq req) {
        String reason = req == null ? null : req.getReason();
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
        return reason.trim();
    }

    /**
     * 解析操作人标识。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 operator 生成的文本或业务键。
     */
    private String operator(TenantContext context) {
        return context != null && context.username() != null && !context.username().isBlank()
                ? context.username()
                : "operator";
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultIfBlank 流程中的校验、计算或对象转换。
     * @return 返回 default if blank 生成的文本或业务键。
     */
    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @return 返回 requiredCanvasService 流程生成的业务结果。
     */
    private CanvasService requiredCanvasService() {
        if (canvasService == null) {
            throw new IllegalStateException("canvasService is not configured");
        }
        return canvasService;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @return 返回 requiredCanvasOpsService 流程生成的业务结果。
     */
    private CanvasOpsService requiredCanvasOpsService() {
        if (canvasOpsService == null) {
            throw new IllegalStateException("canvasOpsService is not configured");
        }
        return canvasOpsService;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @return 返回 requiredAuditService 流程生成的业务结果。
     */
    private OpsAuditEventService requiredAuditService() {
        if (opsAuditEventService == null) {
            throw new IllegalStateException("opsAuditEventService is not configured");
        }
        return opsAuditEventService;
    }
}
