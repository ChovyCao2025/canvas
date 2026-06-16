package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.enums.AudienceSnapshotMode;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.common.tenant.TenantScopeSupport;
import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.chovy.canvas.dal.dataobject.AudienceStatDO;
import org.chovy.canvas.dal.mapper.AudienceStatMapper;
import org.chovy.canvas.domain.notification.NotificationService;
import org.chovy.canvas.dal.dataobject.AsyncTaskDO;
import org.chovy.canvas.domain.task.AsyncTaskCreateResult;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.chovy.canvas.domain.task.AsyncTaskStatus;
import org.chovy.canvas.dto.audience.AudiencePreviewReq;
import org.chovy.canvas.dto.audience.AudiencePreviewResp;
import org.chovy.canvas.dto.audience.AudienceSourceFieldDTO;
import org.chovy.canvas.dto.task.ComputeTaskResp;
import org.chovy.canvas.engine.audience.AudienceBatchComputeService;
import org.chovy.canvas.engine.audience.AudienceComputeTaskRunner;
import org.chovy.canvas.engine.audience.AudienceSchedulerService;
import org.chovy.canvas.engine.audience.CdpAudienceSourceService;
import org.chovy.canvas.infrastructure.concurrent.ManagedVirtualThreadExecutor;
import org.chovy.canvas.perf.PerfRunContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.validation.annotation.Validated;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Audience HTTP 控制器，根路由为 {@code /canvas/audiences}。
 *
 * <p>负责接收前端或外部系统请求，完成参数绑定、基础校验和统一响应包装。
 * <p>具体业务规则委托给领域服务处理，控制器层保持薄封装以减少重复逻辑。
 */
@Slf4j
@RestController
@RequestMapping("/canvas/audiences")
@RequiredArgsConstructor
@Validated
@Tag(name = "Audience", description = "Audience definition, preview, readiness, and compute APIs.")
@SecurityRequirement(name = "bearerAuth")
public class AudienceController {

    /** 人群定义 Mapper，用于读写人群定义。 */
    private final AudienceDefinitionMapper definitionMapper;
    /** 人群统计 Mapper，用于读取人群统计。 */
    private final AudienceStatMapper statMapper;
    /** 人群批量计算服务，用于触发人群计算。 */
    private final AudienceBatchComputeService computeService;
    /** 人群调度服务，用于维护定时计算计划。 */
    private final AudienceSchedulerService schedulerService;
    /** 异步任务服务，用于登记和查询人群计算任务。 */
    private final AsyncTaskService taskService;
    /** 人群计算任务执行器，用于运行后台计算。 */
    private final AudienceComputeTaskRunner computeTaskRunner;
    /** 通知服务，用于发送人群任务通知。 */
    private final NotificationService notificationService;
    /** 承接受众来源和 CDP 画像数据之间的转换查询。 */
    private final CdpAudienceSourceService cdpAudienceSourceService;
    /** 解析当前请求的租户上下文，保证接口按租户隔离读写。 */
    private final TenantContextResolver tenantContextResolver;
    /** 封装租户作用域切换，保证后台受众任务携带正确租户身份。 */
    private final TenantScopeSupport tenantScopeSupport;
    /** 执行受众后台计算任务，避免长耗时计算阻塞请求线程。 */
    private ManagedVirtualThreadExecutor backgroundExecutor = ManagedVirtualThreadExecutor.direct();

    /**
     * 执行 setBackgroundExecutor 流程，围绕 set background executor 完成校验、计算或结果组装。
     *
     * @param backgroundExecutor 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired(required = false)
    void setBackgroundExecutor(ManagedVirtualThreadExecutor backgroundExecutor) {
        this.backgroundExecutor = backgroundExecutor;
    }

    /** 分页查询人群定义。 */
    @GetMapping
    @Operation(operationId = "listAudiences", summary = "List audience definitions")
    public Mono<R<PageResult<AudienceDefinitionDO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return tenantContextResolver.currentOrError().flatMap(context -> Mono.fromCallable(() -> {
            Page<AudienceDefinitionDO> result = definitionMapper.selectPage(
                    new Page<>(page, size),
                    audienceQuery(context).orderByDesc(AudienceDefinitionDO::getId)
            );
            return R.ok(PageResult.of(result.getTotal(), result.getRecords()));
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    /** 查询 CDP 人群数据源可用于圈选的字段。 */
    @GetMapping("/source-fields")
    @Operation(operationId = "listAudienceSourceFields", summary = "List audience source fields")
    public Mono<R<List<AudienceSourceFieldDTO>>> sourceFields(@RequestParam String dataSourceType) {
        return Mono.fromCallable(() -> R.ok(cdpAudienceSourceService.listSourceFields(dataSourceType)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /** 预览 CDP 人群规则命中用户，不保存人群定义。 */
    @PostMapping("/preview")
    @Operation(operationId = "previewAudience", summary = "Preview audience rule matches")
    public Mono<R<AudiencePreviewResp>> preview(@Valid @RequestBody AudiencePreviewReq req) {
        return Mono.fromCallable(() -> {
            if (!cdpAudienceSourceService.supports(req.dataSourceType())) {
                throw new IllegalArgumentException("Unsupported CDP audience source: " + req.dataSourceType());
            }
            List<String> userIds = cdpAudienceSourceService.resolveUserIds(req.dataSourceType(), req.ruleJson());
            int limit = req.sampleLimit() == null ? 10 : Math.max(1, Math.min(req.sampleLimit(), 100));
            return R.ok(new AudiencePreviewResp(userIds.size(), userIds.stream().limit(limit).toList()));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 查询单个人群定义详情。 */
    @GetMapping("/{id}")
    @Operation(operationId = "getAudience", summary = "Get audience definition")
    public Mono<R<AudienceDefinitionDO>> get(@PathVariable Long id) {
        return tenantContextResolver.currentOrError().flatMap(context ->
                Mono.fromCallable(() -> R.ok(requireAudience(id, context)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /** 查询可用于实时判断的“READY 人群”列表。 */
    @GetMapping("/ready")
    @Operation(operationId = "listReadyAudiences", summary = "List ready audiences")
    public Mono<R<List<AudienceDefinitionDO>>> listReady() {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return tenantContextResolver.currentOrError().flatMap(context ->
                Mono.fromCallable(() -> {
                    // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                    List<AudienceStatDO> readyStats = statMapper.selectList(new LambdaQueryWrapper<AudienceStatDO>()
                            .eq(AudienceStatDO::getStatus, "READY"));
                    // 校验关键输入和前置条件，避免无效状态继续进入主流程。
                    if (readyStats.isEmpty()) {
                        return R.ok(List.<AudienceDefinitionDO>of());
                    }
                    List<Long> audienceIds = readyStats.stream().map(AudienceStatDO::getAudienceId).toList();
                    return R.ok(definitionMapper.selectList(audienceQuery(context)
                            .in(AudienceDefinitionDO::getId, audienceIds)
                            .eq(AudienceDefinitionDO::getEnabled, 1)
                            .orderByAsc(AudienceDefinitionDO::getId)));
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    /** 创建人群并触发首次计算，同时注册调度任务。 */
    @PostMapping
    @Operation(operationId = "createAudience", summary = "Create audience definition")
    public Mono<R<AudienceDefinitionDO>> create(@RequestBody AudienceDefinitionDO body) {
        return currentUser().zipWith(tenantContextResolver.currentOrError()).flatMap(tuple ->
                Mono.fromCallable(() -> {
                    String operator = tuple.getT1();
                    applyTenantForWrite(body, tuple.getT2());
                    normalizeDefaultSnapshotMode(body);
                    body.setCreatedBy(operator);
                    AudienceDefinitionDO created = computeService.create(body);
                    schedulerService.refresh(created, () -> computeService.compute(created.getId()));
                    enqueueCompute(created, operator);
                    return R.ok(created);
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    /** 更新人群并触发重算，同时刷新调度任务。 */
    @PutMapping("/{id}")
    @Operation(operationId = "updateAudience", summary = "Update audience definition")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody AudienceDefinitionDO body) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return currentUser().zipWith(tenantContextResolver.currentOrError()).flatMap(tuple ->
                Mono.fromCallable(() -> {
                    String operator = tuple.getT1();
                    TenantContext context = tuple.getT2();
                    requireAudience(id, context);
                    body.setId(id);
                    applyTenantForWrite(body, context);
                    normalizeDefaultSnapshotMode(body);
                    // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                    boolean updated = computeService.update(body);
                    // 校验关键输入和前置条件，避免无效状态继续进入主流程。
                    if (!updated) {
                        /**
                         * 执行 illegalargumentexception 对应的内部处理流程。
                         *
                         * @param id 标识，由调用方提供
                         * @return 返回内部处理结果
                         */
                        throw new IllegalArgumentException("Audience not found: " + id);
                    }
                    AudienceDefinitionDO saved = requireAudience(id, context);
                    schedulerService.refresh(saved, () -> computeService.compute(saved.getId()));
                    enqueueCompute(saved, operator);
                    return R.<Void>ok();
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    /** 删除人群定义、统计数据与调度任务。 */
    @DeleteMapping("/{id}")
    @Operation(operationId = "deleteAudience", summary = "Delete audience definition")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return tenantContextResolver.currentOrError().flatMap(context -> Mono.fromCallable(() -> {
            requireAudience(id, context);
            schedulerService.cancel(id);
            computeService.delete(id);
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    /** 手动触发一次异步计算。 */
    @PostMapping("/{id}/compute")
    @Operation(operationId = "computeAudience", summary = "Compute audience")
    public Mono<R<ComputeTaskResp>> compute(@PathVariable Long id, @RequestBody(required = false) ComputeReq req) {
        String perfRunId = extractPerfRunId(req);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (perfRunId != null) {
            String perfInputId = req == null || req.getPerfInputId() == null || req.getPerfInputId().isBlank()
                    ? null
                    : req.getPerfInputId();
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            return tenantContextResolver.currentOrError().flatMap(context ->
                    Mono.fromCallable(() -> {
                        requireAudience(id, context);
                        backgroundExecutor.submit(
                                "audience-perf-compute-" + id,
                                () -> computeService.compute(id, perfRunId, perfInputId));
                        return R.ok(new ComputeTaskResp(perfTaskId(perfRunId, perfInputId), "QUEUED"));
                    }).subscribeOn(Schedulers.boundedElastic()));
        }

        return currentUser().zipWith(tenantContextResolver.currentOrError()).flatMap(tuple ->
                Mono.fromCallable(() -> {
                    AudienceDefinitionDO definition = requireAudience(id, tuple.getT2());
                    if (definition.getEnabled() == null || definition.getEnabled() == 0) {
                        /**
                         * 执行 illegalstateexception 对应的内部处理流程。
                         *
                         * @param id 标识，由调用方提供
                         * @return 返回内部处理结果
                         */
                        throw new IllegalStateException("Audience disabled: " + id);
                    }
                    // 汇总前面计算出的状态和明细，返回给调用方。
                    return R.ok(enqueueCompute(definition, tuple.getT1()));
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 计算或统计 compute 相关的业务数据。
     *
     * <p>返回值采用 Reactor 异步模型，调用方可继续组合后续处理。
     *
     * @param id id 对应的业务主键或标识
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    public Mono<R<ComputeTaskResp>> compute(Long id) {
        return compute(id, null);
    }

    /** 查询人群计算状态统计。 */
    @GetMapping("/{id}/stat")
    @Operation(operationId = "getAudienceStat", summary = "Get audience statistics")
    public Mono<R<AudienceStatDO>> stat(@PathVariable Long id) {
        return tenantContextResolver.currentOrError().flatMap(context ->
                Mono.fromCallable(() -> {
                    requireAudience(id, context);
                    return R.ok(statMapper.selectById(id));
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 执行 enqueueCompute 流程，围绕 enqueue compute 完成校验、计算或结果组装。
     *
     * @param definition definition 参数，用于 enqueueCompute 流程中的校验、计算或对象转换。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 enqueueCompute 流程生成的业务结果。
     */
    private ComputeTaskResp enqueueCompute(AudienceDefinitionDO definition, String operator) {
        String audienceId = String.valueOf(definition.getId());
        String displayName = displayName(definition);
        AsyncTaskCreateResult result = taskService.createOrReuseRunning(
                "AUDIENCE_COMPUTE",
                "AUDIENCE",
                audienceId,
                "计算人群：" + displayName,
                operator);
        String taskId = result.task().getTaskId();
        if (result.created()) {
            computeTaskRunner.start(taskId, definition.getId(), displayName, operator, definition.getTenantId());
        } else {
            createCatchUpNotificationIfTerminal(result.task(), definition, displayName, operator);
        }
        return new ComputeTaskResp(taskId, result.task().getStatus());
    }

    /**
     * 复用已有终态任务时补发一次任务结果通知。
     *
     * <p>创建或更新人群时如果计算任务已结束，不重新计算，只让当前操作者看到结果入口。
     *
     * @param task 已存在的异步任务
     * @param audienceId 人群定义 ID
     * @param displayName 人群展示名
     * @param operator 当前操作人
     */
    private void createCatchUpNotificationIfTerminal(
            AsyncTaskDO task, AudienceDefinitionDO definition, String displayName, String operator) {
        if (task == null || !isTerminal(task.getStatus())) {
            return;
        }
        Long audienceId = definition.getId();
        String type = AsyncTaskStatus.SUCCEEDED.name().equals(task.getStatus()) ? "TASK_SUCCEEDED" : "TASK_FAILED";
        String title = AsyncTaskStatus.SUCCEEDED.name().equals(task.getStatus()) ? "人群计算完成" : "人群计算失败";
        String detail = AsyncTaskStatus.SUCCEEDED.name().equals(task.getStatus())
                ? "任务已完成"
                : defaultIfBlank(task.getErrorMsg(), "计算失败");
        try {
            notificationService.createForTask(
                    definition.getTenantId(),
                    operator,
                    type,
                    title,
                    displayName + " · " + detail,
                    "/audiences?highlight=" + audienceId + "&taskId=" + task.getTaskId(),
                    task.getTaskId());
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            log.error("[AUDIENCE] failed to create catch-up notification taskId={} user={}: {}",
                    task.getTaskId(), operator, e.getMessage(), e);
        }
    }

    /**
     * 执行 audienceQuery 流程，围绕 audience query 完成校验、计算或结果组装。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 audienceQuery 流程生成的业务结果。
     */
    private LambdaQueryWrapper<AudienceDefinitionDO> audienceQuery(TenantContext context) {
        return tenantScopeSupport.applyTenantFilter(
                new LambdaQueryWrapper<>(),
                AudienceDefinitionDO::getTenantId,
                context);
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 requireAudience 流程生成的业务结果。
     */
    private AudienceDefinitionDO requireAudience(Long id, TenantContext context) {
        AudienceDefinitionDO definition = definitionMapper.selectOne(audienceQuery(context)
                .eq(AudienceDefinitionDO::getId, id)
                .last("LIMIT 1"));
        if (definition == null) {
            /**
             * 执行 illegalargumentexception 对应的内部处理流程。
             *
             * @param id 标识，由调用方提供
             * @return 返回内部处理结果
             */
            throw new IllegalArgumentException("Audience not found: " + id);
        }
        return definition;
    }

    /**
     * 应用请求中的业务字段或租户约束。
     *
     * @param body 待处理业务值，用于规则计算、转换或外部调用。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     */
    private void applyTenantForWrite(AudienceDefinitionDO body, TenantContext context) {
        if (context.tenantId() != null) {
            body.setTenantId(context.tenantId());
            return;
        }
        tenantScopeSupport.applyTenantFilter(new LambdaQueryWrapper<>(), AudienceDefinitionDO::getTenantId, context);
    }

    /**
     * 规范化输入值。
     *
     * @param body 待处理业务值，用于规则计算、转换或外部调用。
     */
    private void normalizeDefaultSnapshotMode(AudienceDefinitionDO body) {
        body.setDefaultSnapshotMode(AudienceSnapshotMode.normalize(body.getDefaultSnapshotMode()).name());
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回布尔判断结果。
     */
    private boolean isTerminal(String status) {
        return AsyncTaskStatus.SUCCEEDED.name().equals(status)
                || AsyncTaskStatus.FAILED.name().equals(status)
                || AsyncTaskStatus.CANCELED.name().equals(status);
    }

    /**
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 current user 生成的文本或业务键。
     */
    private Mono<String> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> {
                    if (ctx.getAuthentication() == null
                            || !(ctx.getAuthentication().getPrincipal() instanceof Claims claims)) {
                        return "system";
                    }
                    return defaultIfBlank(claims.get("username", String.class), "system");
                })
                .defaultIfEmpty("system");
    }

    /**
     * 执行 displayName 流程，围绕 display name 完成校验、计算或结果组装。
     *
     * @param definition definition 参数，用于 displayName 流程中的校验、计算或对象转换。
     * @return 返回 display name 生成的文本或业务键。
     */
    private String displayName(AudienceDefinitionDO definition) {
        return defaultIfBlank(definition.getName(), "人群 " + definition.getId());
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultIfBlank 流程中的校验、计算或对象转换。
     * @return 返回 default if blank 生成的文本或业务键。
     */
    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 执行 perfTaskId 流程，围绕 perf task id 完成校验、计算或结果组装。
     *
     * @param perfRunId 业务对象 ID，用于定位具体记录。
     * @param perfInputId 业务对象 ID，用于定位具体记录。
     * @return 返回 perf task id 生成的文本或业务键。
     */
    private String perfTaskId(String perfRunId, String perfInputId) {
        return "perf:" + defaultIfBlank(perfInputId, perfRunId);
    }

    /**
     * 执行 extractPerfRunId 流程，围绕 extract perf run id 完成校验、计算或结果组装。
     *
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回 extract perf run id 生成的文本或业务键。
     */
    private String extractPerfRunId(ComputeReq req) {
        if (req == null) {
            return null;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("perfRunId", req.getPerfRunId());
        return PerfRunContext.extract(payload);
    }

    @Data
    /**
     * ComputeReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    static class ComputeReq {
        /** 压测运行 ID。 */
        private String perfRunId;
        /** 压测输入 ID。 */
        private String perfInputId;
    }
}
