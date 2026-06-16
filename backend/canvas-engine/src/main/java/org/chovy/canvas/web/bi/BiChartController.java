package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.chart.BiChartResource;
import org.chovy.canvas.domain.bi.chart.BiChartResourceService;
import org.chovy.canvas.domain.bi.chart.BiChartReferenceImpact;
import org.chovy.canvas.domain.bi.chart.BiChartReferenceImpactService;
import org.chovy.canvas.domain.bi.chart.BiChartVersionView;
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

import java.util.List;

/**
 * BiChartController 暴露 web.bi 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/bi/charts/resources")
public class BiChartController {

    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;
    /**
     * 图表资源服务，用于承接对应业务能力和领域编排。
     */
    private final BiChartResourceService chartResourceService;
    /**
     * 图表referenceimpact服务，用于承接对应业务能力和领域编排。
     */
    private final BiChartReferenceImpactService chartReferenceImpactService;

    /**
     * 创建 BiChartController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param chartResourceService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiChartController(TenantContextResolver tenantContextResolver,
                             BiChartResourceService chartResourceService) {
        this(tenantContextResolver, chartResourceService, null);
    }

    /**
     * 创建 BiChartController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param chartResourceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param chartReferenceImpactService 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public BiChartController(TenantContextResolver tenantContextResolver,
                             BiChartResourceService chartResourceService,
                             BiChartReferenceImpactService chartReferenceImpactService) {
        this.tenantContextResolver = tenantContextResolver;
        this.chartResourceService = chartResourceService;
        this.chartReferenceImpactService = chartReferenceImpactService;
    }
    /**
     * 查询 BI 图表列表接口，对应 GET 请求。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 chartResourceService.list 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping
    public Mono<R<List<BiChartResource>>> list() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(chartResourceService.list(context.tenantId())))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 获取 BI 图表详情接口，对应 GET /{chartKey}。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 chartResourceService.get 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param chartKey 图表唯一键。
     * @return 异步返回统一响应，包含获取 BI 图表详情后的业务数据。
     */
    @GetMapping("/{chartKey}")
    public Mono<R<BiChartResource>> get(@PathVariable String chartKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(chartResourceService.get(context.tenantId(), chartKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 BI 图表引用影响接口，对应 GET /{chartKey}/impact。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 chartReferenceImpactService.impact 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param chartKey 图表唯一键。
     * @return 异步返回统一响应，包含查询 BI 图表引用影响后的业务数据。
     */
    @GetMapping("/{chartKey}/impact")
    public Mono<R<BiChartReferenceImpact>> impact(@PathVariable String chartKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(chartReferenceImpactService.impact(context.tenantId(), chartKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 保存 BI 图表配置接口，对应 POST /{chartKey}/draft。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 chartResourceService.saveDraft 完成业务处理。
     * 副作用：会保存配置或运行态。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param chartKey 图表唯一键。
     * @param lockToken 请求头参数，可选。
     * @param resource 请求体，包含本次操作所需字段。
     * @return 异步返回统一响应，包含保存 BI 图表配置后的业务数据。
     */
    @PostMapping("/{chartKey}/draft")
    public Mono<R<BiChartResource>> saveDraft(@PathVariable String chartKey,
                                              @RequestHeader(value = "X-BI-LOCK-TOKEN", required = false) String lockToken,
                                              @RequestBody BiChartResource resource) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (!chartKey.equals(resource.chartKey())) {
                        /**
                         * 执行 illegalargumentexception 对应的内部处理流程。
                         *
                         * @param path" path"，由调用方提供
                         * @return 返回内部处理结果
                         */
                        throw new IllegalArgumentException("chart key does not match request path");
                    }
                    return R.ok(chartResourceService.saveDraft(
                            context.tenantId(),
                            context.username(),
                            context.role(),
                            resource,
                            lockToken));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 保存 BI 图表配置控制器公开辅助方法，供同类 endpoint 复用。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 副作用：会保存配置或运行态。
     * 方法以 Mono 作为异步边界返回，实际执行由 Reactor 链路承载。
     *
     * @param chartKey 图表唯一键。
     * @param resource resource。
     * @return 异步返回统一响应，包含保存 BI 图表配置后的业务数据。
     */
    public Mono<R<BiChartResource>> saveDraft(String chartKey, BiChartResource resource) {
        return saveDraft(chartKey, null, resource);
    }
    /**
     * 发布 BI 图表接口，对应 POST /{chartKey}/publish。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 chartResourceService.publish 完成业务处理。
     * 副作用：会推进发布状态。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param chartKey 图表唯一键。
     * @return 异步返回统一响应，包含发布 BI 图表后的业务数据。
     */
    @PostMapping("/{chartKey}/publish")
    public Mono<R<BiChartResource>> publish(@PathVariable String chartKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(chartResourceService.publish(context.tenantId(), context.username(), context.role(), chartKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 归档 BI 图表接口，对应 DELETE /{chartKey}。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 chartResourceService.archive 完成业务处理。
     * 副作用：会归档资源。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param chartKey 图表唯一键。
     * @return 异步返回统一响应，包含归档 BI 图表后的业务数据。
     */
    @DeleteMapping("/{chartKey}")
    public Mono<R<BiChartResource>> archive(@PathVariable String chartKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(chartResourceService.archive(context.tenantId(), chartKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 BI 图表列表接口，对应 GET /{chartKey}/versions。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 chartResourceService.listVersions 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param chartKey 图表唯一键。
     * @param limit 返回数量上限，默认值为 20。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/{chartKey}/versions")
    public Mono<R<List<BiChartVersionView>>> listVersions(@PathVariable String chartKey,
                                                          @RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(chartResourceService.listVersions(context.tenantId(), chartKey, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 恢复 BI 图表 版本接口，对应 POST /{chartKey}/versions/{version}/restore。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 chartResourceService.restoreVersion 完成业务处理。
     * 副作用：会按指定版本恢复资源。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param chartKey 图表唯一键。
     * @param lockToken 请求头参数，可选。
     * @param version 目标版本号。
     * @return 异步返回统一响应，包含恢复 BI 图表 版本后的业务数据。
     */
    @PostMapping("/{chartKey}/versions/{version}/restore")
    public Mono<R<BiChartResource>> restoreVersion(@PathVariable String chartKey,
                                                   @RequestHeader(value = "X-BI-LOCK-TOKEN", required = false) String lockToken,
                                                   @PathVariable int version) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(chartResourceService.restoreVersion(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                chartKey,
                                version,
                                lockToken)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 恢复 BI 图表 版本控制器公开辅助方法，供同类 endpoint 复用。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 副作用：会按指定版本恢复资源。
     * 方法以 Mono 作为异步边界返回，实际执行由 Reactor 链路承载。
     *
     * @param chartKey 图表唯一键。
     * @param version 目标版本号。
     * @return 异步返回统一响应，包含恢复 BI 图表 版本后的业务数据。
     */
    public Mono<R<BiChartResource>> restoreVersion(String chartKey, int version) {
        return restoreVersion(chartKey, null, version);
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
}
