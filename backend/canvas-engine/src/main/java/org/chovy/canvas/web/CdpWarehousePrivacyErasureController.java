package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService;
import org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyAudienceBitmapRebuildAutomationService;
import org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyAudienceBitmapRebuildService;
import org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyErasureExecutionService;
import org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyErasureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * CdpWarehousePrivacyErasureController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/warehouse/privacy/erasure")
public class CdpWarehousePrivacyErasureController {

    private final CdpWarehousePrivacyErasureService erasureService;
    private final CdpWarehousePrivacyErasureExecutionService executionService;
    private final CdpWarehousePrivacyAudienceBitmapRebuildService rebuildService;
    private final CdpWarehousePrivacyAudienceBitmapRebuildAutomationService automationService;
    private final CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService automationRunService;
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 CdpWarehousePrivacyErasureController 实例并注入 web 场景依赖。
     * @param erasureService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehousePrivacyErasureController(CdpWarehousePrivacyErasureService erasureService) {
        this(erasureService, null, null, null, null, null);
    }

    /**
     * 创建 CdpWarehousePrivacyErasureController 实例并注入 web 场景依赖。
     * @param erasureService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public CdpWarehousePrivacyErasureController(CdpWarehousePrivacyErasureService erasureService,
                                                TenantContextResolver tenantContextResolver) {
        this(erasureService, null, null, null, null, tenantContextResolver);
    }

    /**
     * 创建 CdpWarehousePrivacyErasureController 实例并注入 web 场景依赖。
     * @param erasureService 依赖组件，用于完成数据访问或外部能力调用。
     * @param executionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public CdpWarehousePrivacyErasureController(CdpWarehousePrivacyErasureService erasureService,
                                                CdpWarehousePrivacyErasureExecutionService executionService,
                                                TenantContextResolver tenantContextResolver) {
        this(erasureService, executionService, null, null, null, tenantContextResolver);
    }

    /**
     * 创建 CdpWarehousePrivacyErasureController 实例并注入 web 场景依赖。
     * @param erasureService 依赖组件，用于完成数据访问或外部能力调用。
     * @param executionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param rebuildService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public CdpWarehousePrivacyErasureController(CdpWarehousePrivacyErasureService erasureService,
                                                CdpWarehousePrivacyErasureExecutionService executionService,
                                                CdpWarehousePrivacyAudienceBitmapRebuildService rebuildService,
                                                TenantContextResolver tenantContextResolver) {
        this(erasureService, executionService, rebuildService, null, null, tenantContextResolver);
    }

    /**
     * 创建 CdpWarehousePrivacyErasureController 实例并注入 web 场景依赖。
     * @param erasureService 依赖组件，用于完成数据访问或外部能力调用。
     * @param executionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param rebuildService 依赖组件，用于完成数据访问或外部能力调用。
     * @param automationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public CdpWarehousePrivacyErasureController(CdpWarehousePrivacyErasureService erasureService,
                                                CdpWarehousePrivacyErasureExecutionService executionService,
                                                CdpWarehousePrivacyAudienceBitmapRebuildService rebuildService,
                                                CdpWarehousePrivacyAudienceBitmapRebuildAutomationService automationService,
                                                TenantContextResolver tenantContextResolver) {
        this(erasureService, executionService, rebuildService, automationService, null, tenantContextResolver);
    }

    /**
     * 创建 CdpWarehousePrivacyErasureController 实例并注入 web 场景依赖。
     * @param erasureService 依赖组件，用于完成数据访问或外部能力调用。
     * @param executionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param rebuildService 依赖组件，用于完成数据访问或外部能力调用。
     * @param automationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param automationRunService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public CdpWarehousePrivacyErasureController(CdpWarehousePrivacyErasureService erasureService,
                                                CdpWarehousePrivacyErasureExecutionService executionService,
                                                CdpWarehousePrivacyAudienceBitmapRebuildService rebuildService,
                                                CdpWarehousePrivacyAudienceBitmapRebuildAutomationService automationService,
                                                CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService automationRunService,
                                                TenantContextResolver tenantContextResolver) {
        this.erasureService = erasureService;
        this.executionService = executionService;
        this.rebuildService = rebuildService;
        this.automationService = automationService;
        this.automationRunService = automationRunService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 创建 CDP 数仓 Privacy Erasure接口，对应 POST /requests。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 erasureService.create 完成业务处理。
     * 副作用：会写入新记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建 CDP 数仓 Privacy Erasure后的业务数据。
     */
    @PostMapping("/requests")
    public Mono<R<CdpWarehousePrivacyErasureService.ErasureRequestView>> create(
            @RequestBody CdpWarehousePrivacyErasureService.ErasureRequestCommand command) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(erasureService.create(tenantId, command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 记录CDP 数仓 Privacy Erasure数据接口，对应 POST /requests/{id}/proofs。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 erasureService.recordAssetProof 完成业务处理。
     * 副作用：会记录业务快照。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含记录CDP 数仓 Privacy Erasure数据后的业务数据。
     */
    @PostMapping("/requests/{id}/proofs")
    public Mono<R<CdpWarehousePrivacyErasureService.ErasureRequestView>> recordProof(
            @PathVariable Long id,
            @RequestBody CdpWarehousePrivacyErasureService.AssetProofCommand command) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(erasureService.recordAssetProof(tenantId, id, command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 执行 CDP 数仓 Privacy Erasure查询接口，对应 POST /requests/{id}/execute。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 executionService.execute 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含执行 CDP 数仓 Privacy Erasure查询后的业务数据。
     */
    @PostMapping("/requests/{id}/execute")
    public Mono<R<CdpWarehousePrivacyErasureExecutionService.ErasureExecutionResult>> execute(
            @PathVariable Long id,
            @RequestBody CdpWarehousePrivacyErasureExecutionService.ErasureExecutionCommand command) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> {
                    if (executionService == null) {
                        throw new IllegalStateException("privacy erasure execution service is not configured");
                    }
                    return R.ok(executionService.execute(tenantId, id, command));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Privacy Erasure 请求接口，对应 POST /requests/{id}/audience-rebuild。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 rebuildService.rebuild 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含处理 CDP 数仓 Privacy Erasure 请求后的业务数据。
     */
    @PostMapping("/requests/{id}/audience-rebuild")
    public Mono<R<CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildResult>> rebuildAudienceBitmaps(
            @PathVariable Long id,
            @RequestBody CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildCommand command) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> {
                    if (rebuildService == null) {
                        throw new IllegalStateException("privacy audience bitmap rebuild service is not configured");
                    }
                    return R.ok(rebuildService.rebuild(tenantId, id, command));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * runAudienceBitmapRebuildAutomation 创建或触发 web 场景的业务处理。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    @PostMapping("/audience-rebuild/automation/run")
    public Mono<R<CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult>>
    runAudienceBitmapRebuildAutomation(
            @RequestBody(required = false)
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand command) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> {
                    if (automationRunService != null) {
                        return R.ok(automationRunService.runAndRecord(tenantId, command, "MANUAL").result());
                    }
                    if (automationService == null) {
                        throw new IllegalStateException(
                                "privacy audience bitmap rebuild automation service is not configured");
                    }
                    return R.ok(automationService.run(tenantId, command));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * recentAudienceBitmapRebuildAutomationRuns 处理 web 场景的业务逻辑。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    @GetMapping("/audience-rebuild/automation/runs")
    public Mono<R<List<CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService.AutomationRunView>>>
    recentAudienceBitmapRebuildAutomationRuns(@RequestParam(defaultValue = "20") int limit) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> {
                    if (automationRunService == null) {
                        throw new IllegalStateException(
                                "privacy audience bitmap rebuild automation run service is not configured");
                    }
                    return R.ok(automationRunService.recent(tenantId, limit));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * getAudienceBitmapRebuildAutomationRun 查询 web 场景的业务数据。
     * @param id 业务对象 ID，用于定位具体记录。
     * @return 返回 getAudienceBitmapRebuildAutomationRun 流程生成的业务结果。
     */
    @GetMapping("/audience-rebuild/automation/runs/{id}")
    public Mono<R<CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService.AutomationRunView>>
    getAudienceBitmapRebuildAutomationRun(@PathVariable Long id) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> {
                    if (automationRunService == null) {
                        throw new IllegalStateException(
                                "privacy audience bitmap rebuild automation run service is not configured");
                    }
                    return R.ok(automationRunService.get(tenantId, id));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Privacy Erasure 请求接口，对应 GET /requests。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 erasureService.recent 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param status 状态过滤条件，可选。
     * @param limit 返回数量上限，默认值为 20。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/requests")
    public Mono<R<List<CdpWarehousePrivacyErasureService.ErasureRequestView>>> recent(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(erasureService.recent(tenantId, status, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 获取 CDP 数仓 Privacy Erasure详情接口，对应 GET /requests/{id}。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 erasureService.get 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，包含获取 CDP 数仓 Privacy Erasure详情后的业务数据。
     */
    @GetMapping("/requests/{id}")
    public Mono<R<CdpWarehousePrivacyErasureService.ErasureRequestView>> get(@PathVariable Long id) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(erasureService.get(tenantId, id)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 CDP 数仓 Privacy Erasure汇总接口，对应 GET /summary。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 erasureService.summary 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @return 异步返回统一响应，包含查询 CDP 数仓 Privacy Erasure汇总后的业务数据。
     */
    @GetMapping("/summary")
    public Mono<R<CdpWarehousePrivacyErasureService.BacklogSummary>> summary() {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(erasureService.summary(tenantId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 current tenant id 计算得到的数量、金额或指标值。
     */
    private Mono<Long> currentTenantId() {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (tenantContextResolver == null) {
            return Mono.just(0L);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return tenantContextResolver.current()
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .map(context -> context.tenantId() == null ? 0L : context.tenantId())
                .defaultIfEmpty(0L)
                .map(tenantId -> tenantId == null ? 0L : tenantId);
    }
}
