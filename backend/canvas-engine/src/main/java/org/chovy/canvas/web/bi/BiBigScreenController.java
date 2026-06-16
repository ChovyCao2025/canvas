package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.bigscreen.BiBigScreenResource;
import org.chovy.canvas.domain.bi.bigscreen.BiBigScreenResourceService;
import org.chovy.canvas.domain.bi.bigscreen.BiBigScreenVersionView;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * BiBigScreenController 暴露 web.bi 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/bi/big-screens/resources")
public class BiBigScreenController {

    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;
    /**
     * bigscreen资源服务，用于承接对应业务能力和领域编排。
     */
    private final BiBigScreenResourceService bigScreenResourceService;

    /**
     * 创建 BiBigScreenController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param bigScreenResourceService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiBigScreenController(TenantContextResolver tenantContextResolver,
                                 BiBigScreenResourceService bigScreenResourceService) {
        this.tenantContextResolver = tenantContextResolver;
        this.bigScreenResourceService = bigScreenResourceService;
    }
    /**
     * 查询 BI 大屏列表接口，对应 GET 请求。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 bigScreenResourceService.list 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping
    public Mono<R<List<BiBigScreenResource>>> list() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(bigScreenResourceService.list(context.tenantId())))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 获取 BI 大屏详情接口，对应 GET /{screenKey}。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 bigScreenResourceService.get 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param screenKey 大屏唯一键。
     * @return 异步返回统一响应，包含获取 BI 大屏详情后的业务数据。
     */
    @GetMapping("/{screenKey}")
    public Mono<R<BiBigScreenResource>> get(@PathVariable String screenKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(bigScreenResourceService.get(context.tenantId(), screenKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 保存 BI 大屏配置接口，对应 POST /{screenKey}/draft。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 bigScreenResourceService.saveDraft 完成业务处理。
     * 副作用：会保存配置或运行态。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param screenKey 大屏唯一键。
     * @param resource 请求体，包含本次操作所需字段。
     * @return 异步返回统一响应，包含保存 BI 大屏配置后的业务数据。
     */
    @PostMapping("/{screenKey}/draft")
    public Mono<R<BiBigScreenResource>> saveDraft(@PathVariable String screenKey,
                                                  @RequestBody BiBigScreenResource resource) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (!screenKey.equals(resource.screenKey())) {
                        /**
                         * 执行 illegalargumentexception 对应的内部处理流程。
                         *
                         * @param path" path"，由调用方提供
                         * @return 返回内部处理结果
                         */
                        throw new IllegalArgumentException("big screen key does not match request path");
                    }
                    return R.ok(bigScreenResourceService.saveDraft(
                            context.tenantId(),
                            context.username(),
                            resource));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 发布 BI 大屏接口，对应 POST /{screenKey}/publish。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 bigScreenResourceService.publish 完成业务处理。
     * 副作用：会推进发布状态。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param screenKey 大屏唯一键。
     * @return 异步返回统一响应，包含发布 BI 大屏后的业务数据。
     */
    @PostMapping("/{screenKey}/publish")
    public Mono<R<BiBigScreenResource>> publish(@PathVariable String screenKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(bigScreenResourceService.publish(context.tenantId(), context.username(), screenKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 归档 BI 大屏接口，对应 DELETE /{screenKey}。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 bigScreenResourceService.archive 完成业务处理。
     * 副作用：会归档资源。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param screenKey 大屏唯一键。
     * @return 异步返回统一响应，包含归档 BI 大屏后的业务数据。
     */
    @DeleteMapping("/{screenKey}")
    public Mono<R<BiBigScreenResource>> archive(@PathVariable String screenKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(bigScreenResourceService.archive(context.tenantId(), screenKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 BI 大屏列表接口，对应 GET /{screenKey}/versions。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 bigScreenResourceService.listVersions 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param screenKey 大屏唯一键。
     * @param limit 返回数量上限，默认值为 20。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/{screenKey}/versions")
    public Mono<R<List<BiBigScreenVersionView>>> listVersions(@PathVariable String screenKey,
                                                              @RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(bigScreenResourceService.listVersions(context.tenantId(), screenKey, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 恢复 BI 大屏 版本接口，对应 POST /{screenKey}/versions/{version}/restore。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 bigScreenResourceService.restoreVersion 完成业务处理。
     * 副作用：会按指定版本恢复资源。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param screenKey 大屏唯一键。
     * @param version 目标版本号。
     * @return 异步返回统一响应，包含恢复 BI 大屏 版本后的业务数据。
     */
    @PostMapping("/{screenKey}/versions/{version}/restore")
    public Mono<R<BiBigScreenResource>> restoreVersion(@PathVariable String screenKey,
                                                       @PathVariable int version) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(bigScreenResourceService.restoreVersion(
                                context.tenantId(),
                                context.username(),
                                screenKey,
                                version)))
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
}
