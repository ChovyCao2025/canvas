package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationPolicyCommand;
import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationPolicyView;
import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationSchedulerResult;
import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationSchedulerService;
import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationService;
import org.chovy.canvas.domain.bi.dataset.BiDatasetExtractCapacitySummaryView;
import org.chovy.canvas.domain.bi.dataset.BiDatasetExtractCleanupResultView;
import org.chovy.canvas.domain.bi.dataset.BiDatasetExtractRefreshRunView;
import org.chovy.canvas.domain.bi.dataset.BiDatasetFromDatasourceCommand;
import org.chovy.canvas.domain.bi.dataset.BiDatasetFromDatasourceMultiTableCommand;
import org.chovy.canvas.domain.bi.dataset.BiDatasetFromDatasourceService;
import org.chovy.canvas.domain.bi.dataset.BiDatasetResource;
import org.chovy.canvas.domain.bi.dataset.BiDatasetResourceService;
import org.chovy.canvas.domain.bi.dataset.BiDatasetVersionView;
import org.chovy.canvas.domain.bi.dataset.BiSqlDatasetPreviewCommand;
import org.chovy.canvas.domain.bi.dataset.BiSqlDatasetPreviewResult;
import org.chovy.canvas.domain.bi.dataset.BiSqlDatasetPreviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BiDatasetController 暴露 web.bi 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/bi/datasets/resources")
public class BiDatasetController {

    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;
    /**
     * 数据集资源服务，用于承接对应业务能力和领域编排。
     */
    private final BiDatasetResourceService datasetResourceService;
    /**
     * datasource服务，用于承接对应业务能力和领域编排。
     */
    private final BiDatasetFromDatasourceService datasourceService;
    /**
     * acceleration服务，用于承接对应业务能力和领域编排。
     */
    private final BiDatasetAccelerationService accelerationService;
    /**
     * accelerationscheduler服务，用于承接对应业务能力和领域编排。
     */
    private final BiDatasetAccelerationSchedulerService accelerationSchedulerService;
    /**
     * sql数据集preview服务，用于承接对应业务能力和领域编排。
     */
    private final BiSqlDatasetPreviewService sqlDatasetPreviewService;

    /**
     * 创建 BiDatasetController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param datasetResourceService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDatasetController(TenantContextResolver tenantContextResolver,
                               BiDatasetResourceService datasetResourceService) {
        this(tenantContextResolver, datasetResourceService, null, null, null);
    }

    /**
     * 创建 BiDatasetController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param datasetResourceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasourceService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDatasetController(TenantContextResolver tenantContextResolver,
                               BiDatasetResourceService datasetResourceService,
                               BiDatasetFromDatasourceService datasourceService) {
        this(tenantContextResolver, datasetResourceService, datasourceService, null, null);
    }

    /**
     * 创建 BiDatasetController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param datasetResourceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasourceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param accelerationService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDatasetController(TenantContextResolver tenantContextResolver,
                               BiDatasetResourceService datasetResourceService,
                               BiDatasetFromDatasourceService datasourceService,
                               BiDatasetAccelerationService accelerationService) {
        this(tenantContextResolver, datasetResourceService, datasourceService, accelerationService, null);
    }

    /**
     * 创建 BiDatasetController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param datasetResourceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasourceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param accelerationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param accelerationSchedulerService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDatasetController(TenantContextResolver tenantContextResolver,
                               BiDatasetResourceService datasetResourceService,
                               BiDatasetFromDatasourceService datasourceService,
                               BiDatasetAccelerationService accelerationService,
                               BiDatasetAccelerationSchedulerService accelerationSchedulerService) {
        this(tenantContextResolver, datasetResourceService, datasourceService, accelerationService,
                accelerationSchedulerService, null);
    }

    /**
     * 创建 BiDatasetController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param datasetResourceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasourceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param accelerationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param accelerationSchedulerService 依赖组件，用于完成数据访问或外部能力调用。
     * @param sqlDatasetPreviewService 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public BiDatasetController(TenantContextResolver tenantContextResolver,
                               BiDatasetResourceService datasetResourceService,
                               BiDatasetFromDatasourceService datasourceService,
                               BiDatasetAccelerationService accelerationService,
                               BiDatasetAccelerationSchedulerService accelerationSchedulerService,
                               BiSqlDatasetPreviewService sqlDatasetPreviewService) {
        this.tenantContextResolver = tenantContextResolver;
        this.datasetResourceService = datasetResourceService;
        this.datasourceService = datasourceService;
        this.accelerationService = accelerationService;
        this.accelerationSchedulerService = accelerationSchedulerService;
        this.sqlDatasetPreviewService = sqlDatasetPreviewService;
    }
    /**
     * 查询 BI 数据集列表接口，对应 GET 请求。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 datasetResourceService.listResources 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping
    public Mono<R<List<BiDatasetResource>>> list() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(datasetResourceService.listResources(context.tenantId())))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 获取 BI 数据集详情接口，对应 GET /{datasetKey}。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 datasetResourceService.getResource 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param datasetKey 数据集唯一键。
     * @return 异步返回统一响应，包含获取 BI 数据集详情后的业务数据。
     */
    @GetMapping("/{datasetKey}")
    public Mono<R<BiDatasetResource>> get(@PathVariable String datasetKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(datasetResourceService.getResource(context.tenantId(), datasetKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 保存 BI 数据集配置接口，对应 POST /{datasetKey}/draft。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 datasetResourceService.saveDraft 完成业务处理。
     * 副作用：会保存配置或运行态。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param datasetKey 数据集唯一键。
     * @param lockToken 请求头参数，可选。
     * @param resource 请求体，包含本次操作所需字段。
     * @return 异步返回统一响应，包含保存 BI 数据集配置后的业务数据。
     */
    @PostMapping("/{datasetKey}/draft")
    public Mono<R<BiDatasetResource>> saveDraft(@PathVariable String datasetKey,
                                                @RequestHeader(value = "X-BI-LOCK-TOKEN", required = false) String lockToken,
                                                @RequestBody BiDatasetResource resource) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (!datasetKey.equals(resource.datasetKey())) {
                        /**
                         * 执行 illegalargumentexception 对应的内部处理流程。
                         *
                         * @param path" path"，由调用方提供
                         * @return 返回内部处理结果
                         */
                        throw new IllegalArgumentException("dataset key does not match request path");
                    }
                    return R.ok(datasetResourceService.saveDraft(
                            context.tenantId(),
                            context.username(),
                            context.role(),
                            resource,
                            lockToken));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 保存 BI 数据集配置控制器公开辅助方法，供同类 endpoint 复用。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 副作用：会保存配置或运行态。
     * 方法以 Mono 作为异步边界返回，实际执行由 Reactor 链路承载。
     *
     * @param datasetKey 数据集唯一键。
     * @param resource resource。
     * @return 异步返回统一响应，包含保存 BI 数据集配置后的业务数据。
     */
    public Mono<R<BiDatasetResource>> saveDraft(String datasetKey, BiDatasetResource resource) {
        return saveDraft(datasetKey, null, resource);
    }
    /**
     * 发布 BI 数据集接口，对应 POST /{datasetKey}/publish。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 datasetResourceService.publish 完成业务处理。
     * 副作用：会推进发布状态。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param datasetKey 数据集唯一键。
     * @return 异步返回统一响应，包含发布 BI 数据集后的业务数据。
     */
    @PostMapping("/{datasetKey}/publish")
    public Mono<R<BiDatasetResource>> publish(@PathVariable String datasetKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(datasetResourceService.publish(context.tenantId(), context.username(), context.role(), datasetKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 归档 BI 数据集接口，对应 DELETE /{datasetKey}。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 datasetResourceService.archive 完成业务处理。
     * 副作用：会归档资源。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param datasetKey 数据集唯一键。
     * @return 异步返回统一响应，包含归档 BI 数据集后的业务数据。
     */
    @DeleteMapping("/{datasetKey}")
    public Mono<R<BiDatasetResource>> archive(@PathVariable String datasetKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(datasetResourceService.archive(context.tenantId(), datasetKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 BI 数据集列表接口，对应 GET /{datasetKey}/versions。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 datasetResourceService.listVersions 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param datasetKey 数据集唯一键。
     * @param limit 返回数量上限，默认值为 20。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/{datasetKey}/versions")
    public Mono<R<List<BiDatasetVersionView>>> listVersions(@PathVariable String datasetKey,
                                                            @RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(datasetResourceService.listVersions(context.tenantId(), datasetKey, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 恢复 BI 数据集 版本接口，对应 POST /{datasetKey}/versions/{version}/restore。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 datasetResourceService.restoreVersion 完成业务处理。
     * 副作用：会按指定版本恢复资源。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param datasetKey 数据集唯一键。
     * @param lockToken 请求头参数，可选。
     * @param version 目标版本号。
     * @return 异步返回统一响应，包含恢复 BI 数据集 版本后的业务数据。
     */
    @PostMapping("/{datasetKey}/versions/{version}/restore")
    public Mono<R<BiDatasetResource>> restoreVersion(@PathVariable String datasetKey,
                                                     @RequestHeader(value = "X-BI-LOCK-TOKEN", required = false) String lockToken,
                                                     @PathVariable int version) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(datasetResourceService.restoreVersion(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                datasetKey,
                                version,
                                lockToken)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 恢复 BI 数据集 版本控制器公开辅助方法，供同类 endpoint 复用。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 副作用：会按指定版本恢复资源。
     * 方法以 Mono 作为异步边界返回，实际执行由 Reactor 链路承载。
     *
     * @param datasetKey 数据集唯一键。
     * @param version 目标版本号。
     * @return 异步返回统一响应，包含恢复 BI 数据集 版本后的业务数据。
     */
    public Mono<R<BiDatasetResource>> restoreVersion(String datasetKey, int version) {
        return restoreVersion(datasetKey, null, version);
    }
    /**
     * 处理 BI 数据集 请求接口，对应 GET /{datasetKey}/acceleration-policy。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 accelerationService.policyView 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param datasetKey 数据集唯一键。
     * @return 异步返回统一响应，包含处理 BI 数据集 请求后的业务数据。
     */
    @GetMapping("/{datasetKey}/acceleration-policy")
    public Mono<R<BiDatasetAccelerationPolicyView>> accelerationPolicy(@PathVariable String datasetKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    requireAccelerationService();
                    return R.ok(accelerationService.policyView(context.tenantId(), datasetKey));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建或更新 BI 数据集接口，对应 POST /{datasetKey}/acceleration-policy。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 accelerationService.upsertPolicy 完成业务处理。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param datasetKey 数据集唯一键。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建或更新 BI 数据集后的业务数据。
     */
    @PostMapping("/{datasetKey}/acceleration-policy")
    public Mono<R<BiDatasetAccelerationPolicyView>> upsertAccelerationPolicy(
            @PathVariable String datasetKey,
            @RequestBody BiDatasetAccelerationPolicyCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    requireAccelerationService();
                    return R.ok(accelerationService.upsertPolicy(
                            context.tenantId(),
                            datasetKey,
                            command,
                            context.username()));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 刷新 BI 数据集接口，对应 POST /{datasetKey}/acceleration-refresh。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 accelerationService.refreshNow 完成业务处理。
     * 副作用：会触发刷新流程。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param datasetKey 数据集唯一键。
     * @return 异步返回统一响应，包含刷新 BI 数据集后的业务数据。
     */
    @PostMapping("/{datasetKey}/acceleration-refresh")
    public Mono<R<BiDatasetExtractRefreshRunView>> refreshAcceleration(@PathVariable String datasetKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    requireAccelerationService();
                    return R.ok(accelerationService.refreshNow(context.tenantId(), datasetKey, context.username()));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 触发 BI 数据集运行接口，对应 POST /acceleration-scheduler/run。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 accelerationSchedulerService.runDueOnce 完成业务处理。
     * 副作用：会触发一次运行流程。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @return 异步返回统一响应，包含触发 BI 数据集运行后的业务数据。
     */
    @PostMapping("/acceleration-scheduler/run")
    public Mono<R<BiDatasetAccelerationSchedulerResult>> runAccelerationScheduler() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    requireAccelerationSchedulerService();
                    return R.ok(accelerationSchedulerService.runDueOnce(
                            context.tenantId(),
                            context.username(),
                            LocalDateTime.now()));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 预览 BI 数据集结果接口，对应 POST /sql-preview。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 sqlDatasetPreviewService.preview 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含预览 BI 数据集结果后的业务数据。
     */
    @PostMapping("/sql-preview")
    public Mono<R<BiSqlDatasetPreviewResult>> previewSqlDataset(@RequestBody BiSqlDatasetPreviewCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    requireSqlDatasetPreviewService();
                    return R.ok(sqlDatasetPreviewService.preview(context.tenantId(), command));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 触发 BI 数据集运行接口，对应 GET /{datasetKey}/acceleration-runs。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 accelerationService.recentRuns 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param datasetKey 数据集唯一键。
     * @param limit 返回数量上限，默认值为 10。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/{datasetKey}/acceleration-runs")
    public Mono<R<List<BiDatasetExtractRefreshRunView>>> accelerationRuns(@PathVariable String datasetKey,
                                                                          @RequestParam(defaultValue = "10") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    requireAccelerationService();
                    return R.ok(accelerationService.recentRuns(context.tenantId(), datasetKey, limit));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 BI 数据集 请求接口，对应 GET /{datasetKey}/acceleration-capacity。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 accelerationService.capacitySummary 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param datasetKey 数据集唯一键。
     * @param limit 返回数量上限，默认值为 50。
     * @return 异步返回统一响应，包含处理 BI 数据集 请求后的业务数据。
     */
    @GetMapping("/{datasetKey}/acceleration-capacity")
    public Mono<R<BiDatasetExtractCapacitySummaryView>> accelerationCapacity(
            @PathVariable String datasetKey,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    requireAccelerationService();
                    return R.ok(accelerationService.capacitySummary(context.tenantId(), datasetKey, limit));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 清理 BI 数据集数据接口，对应 POST /{datasetKey}/acceleration-cleanup。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 accelerationService.cleanupRetainedExtracts 完成业务处理。
     * 副作用：会清理过期或无效数据。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param datasetKey 数据集唯一键。
     * @param retainTables 请求参数，默认值为 2。
     * @return 异步返回统一响应，包含清理 BI 数据集数据后的业务数据。
     */
    @PostMapping("/{datasetKey}/acceleration-cleanup")
    public Mono<R<BiDatasetExtractCleanupResultView>> cleanupAcceleration(
            @PathVariable String datasetKey,
            @RequestParam(defaultValue = "2") int retainTables) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    requireAccelerationService();
                    return R.ok(accelerationService.cleanupRetainedExtracts(context.tenantId(), datasetKey, retainTables));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建 BI 数据集接口，对应 POST /from-datasource-schema。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 datasourceService.createTableDataset 完成业务处理。
     * 副作用：会写入新记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建 BI 数据集后的业务数据。
     */
    @PostMapping("/from-datasource-schema")
    public Mono<R<BiDatasetResource>> createFromDatasourceSchema(@RequestBody BiDatasetFromDatasourceCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (datasourceService == null) {
                        /**
                         * 执行 illegalstateexception 对应的内部处理流程。
                         *
                         * @param required" required"，由调用方提供
                         * @return 返回内部处理结果
                         */
                        throw new IllegalStateException("BI datasource dataset service is required");
                    }
                    return R.ok(datasourceService.createTableDataset(
                            context.tenantId(),
                            context.username(),
                            context.role(),
                            command));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建 BI 数据集接口，对应 POST /from-datasource-schema/multi-table。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 datasourceService.createMultiTableDataset 完成业务处理。
     * 副作用：会写入新记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建 BI 数据集后的业务数据。
     */
    @PostMapping("/from-datasource-schema/multi-table")
    public Mono<R<BiDatasetResource>> createMultiTableFromDatasourceSchema(
            @RequestBody BiDatasetFromDatasourceMultiTableCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (datasourceService == null) {
                        /**
                         * 执行 illegalstateexception 对应的内部处理流程。
                         *
                         * @param required" required"，由调用方提供
                         * @return 返回内部处理结果
                         */
                        throw new IllegalStateException("BI datasource dataset service is required");
                    }
                    return R.ok(datasourceService.createMultiTableDataset(
                            context.tenantId(),
                            context.username(),
                            context.role(),
                            command));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 currentTenant 流程生成的业务结果。
     */
    private Mono<TenantContext> currentTenant() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, null, "system"));
        }
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(0L, null, "system"));
    }

    /**
     * 校验并获取必需参数、资源或权限。
     */
    private void requireAccelerationService() {
        if (accelerationService == null) {
            /**
             * 执行 illegalstateexception 对应的内部处理流程。
             *
             * @param required" required"，由调用方提供
             * @return 返回内部处理结果
             */
            throw new IllegalStateException("BI dataset acceleration service is required");
        }
    }

    /**
     * 校验并获取必需参数、资源或权限。
     */
    private void requireAccelerationSchedulerService() {
        if (accelerationSchedulerService == null) {
            /**
             * 执行 illegalstateexception 对应的内部处理流程。
             *
             * @param required" required"，由调用方提供
             * @return 返回内部处理结果
             */
            throw new IllegalStateException("BI dataset acceleration scheduler service is required");
        }
    }

    /**
     * 校验并获取必需参数、资源或权限。
     */
    private void requireSqlDatasetPreviewService() {
        if (sqlDatasetPreviewService == null) {
            /**
             * 执行 illegalstateexception 对应的内部处理流程。
             *
             * @param required" required"，由调用方提供
             * @return 返回内部处理结果
             */
            throw new IllegalStateException("BI SQL dataset preview service is required");
        }
    }
}
