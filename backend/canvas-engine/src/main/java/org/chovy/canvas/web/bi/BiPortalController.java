package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.portal.BiPortalResource;
import org.chovy.canvas.domain.bi.portal.BiPortalResourceService;
import org.chovy.canvas.domain.bi.portal.BiPortalVersionView;
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
 * BiPortalController 暴露 web.bi 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/bi/portals/resources")
public class BiPortalController {

    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;
    /**
     * 门户资源服务，用于承接对应业务能力和领域编排。
     */
    private final BiPortalResourceService portalResourceService;

    /**
     * 创建 BiPortalController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param portalResourceService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiPortalController(TenantContextResolver tenantContextResolver,
                              BiPortalResourceService portalResourceService) {
        this.tenantContextResolver = tenantContextResolver;
        this.portalResourceService = portalResourceService;
    }
    /**
     * 查询 BI 门户列表接口，对应 GET 请求。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 portalResourceService.list 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping
    public Mono<R<List<BiPortalResource>>> list() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(portalResourceService.list(context.tenantId())))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 获取 BI 门户详情接口，对应 GET /{portalKey}。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 portalResourceService.get 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param portalKey 门户唯一键。
     * @return 异步返回统一响应，包含获取 BI 门户详情后的业务数据。
     */
    @GetMapping("/{portalKey}")
    public Mono<R<BiPortalResource>> get(@PathVariable String portalKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(portalResourceService.get(context.tenantId(), portalKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 保存 BI 门户配置接口，对应 POST /{portalKey}/draft。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 portalResourceService.saveDraft 完成业务处理。
     * 副作用：会保存配置或运行态。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param portalKey 门户唯一键。
     * @param lockToken 请求头参数，可选。
     * @param resource 请求体，包含本次操作所需字段。
     * @return 异步返回统一响应，包含保存 BI 门户配置后的业务数据。
     */
    @PostMapping("/{portalKey}/draft")
    public Mono<R<BiPortalResource>> saveDraft(@PathVariable String portalKey,
                                               @RequestHeader(value = "X-BI-LOCK-TOKEN", required = false) String lockToken,
                                               @RequestBody BiPortalResource resource) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (!portalKey.equals(resource.portalKey())) {
                        /**
                         * 执行 illegalargumentexception 对应的内部处理流程。
                         *
                         * @param path" path"，由调用方提供
                         * @return 返回内部处理结果
                         */
                        throw new IllegalArgumentException("portal key does not match request path");
                    }
                    return R.ok(portalResourceService.saveDraft(
                            context.tenantId(),
                            context.username(),
                            context.role(),
                            resource,
                            lockToken));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 保存 BI 门户配置控制器公开辅助方法，供同类 endpoint 复用。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 副作用：会保存配置或运行态。
     * 方法以 Mono 作为异步边界返回，实际执行由 Reactor 链路承载。
     *
     * @param portalKey 门户唯一键。
     * @param resource resource。
     * @return 异步返回统一响应，包含保存 BI 门户配置后的业务数据。
     */
    public Mono<R<BiPortalResource>> saveDraft(String portalKey, BiPortalResource resource) {
        return saveDraft(portalKey, null, resource);
    }
    /**
     * 发布 BI 门户接口，对应 POST /{portalKey}/publish。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 portalResourceService.publish 完成业务处理。
     * 副作用：会推进发布状态。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param portalKey 门户唯一键。
     * @return 异步返回统一响应，包含发布 BI 门户后的业务数据。
     */
    @PostMapping("/{portalKey}/publish")
    public Mono<R<BiPortalResource>> publish(@PathVariable String portalKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(portalResourceService.publish(context.tenantId(), context.username(), context.role(), portalKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 归档 BI 门户接口，对应 DELETE /{portalKey}。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 portalResourceService.archive 完成业务处理。
     * 副作用：会归档资源。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param portalKey 门户唯一键。
     * @return 异步返回统一响应，包含归档 BI 门户后的业务数据。
     */
    @DeleteMapping("/{portalKey}")
    public Mono<R<BiPortalResource>> archive(@PathVariable String portalKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(portalResourceService.archive(context.tenantId(), portalKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 BI 门户列表接口，对应 GET /{portalKey}/versions。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 portalResourceService.listVersions 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param portalKey 门户唯一键。
     * @param limit 返回数量上限，默认值为 20。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/{portalKey}/versions")
    public Mono<R<List<BiPortalVersionView>>> listVersions(@PathVariable String portalKey,
                                                           @RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(portalResourceService.listVersions(context.tenantId(), portalKey, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 恢复 BI 门户 版本接口，对应 POST /{portalKey}/versions/{version}/restore。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 portalResourceService.restoreVersion 完成业务处理。
     * 副作用：会按指定版本恢复资源。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param portalKey 门户唯一键。
     * @param lockToken 请求头参数，可选。
     * @param version 目标版本号。
     * @return 异步返回统一响应，包含恢复 BI 门户 版本后的业务数据。
     */
    @PostMapping("/{portalKey}/versions/{version}/restore")
    public Mono<R<BiPortalResource>> restoreVersion(@PathVariable String portalKey,
                                                    @RequestHeader(value = "X-BI-LOCK-TOKEN", required = false) String lockToken,
                                                    @PathVariable int version) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(portalResourceService.restoreVersion(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                portalKey,
                                version,
                                lockToken)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 恢复 BI 门户 版本控制器公开辅助方法，供同类 endpoint 复用。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 副作用：会按指定版本恢复资源。
     * 方法以 Mono 作为异步边界返回，实际执行由 Reactor 链路承载。
     *
     * @param portalKey 门户唯一键。
     * @param version 目标版本号。
     * @return 异步返回统一响应，包含恢复 BI 门户 版本后的业务数据。
     */
    public Mono<R<BiPortalResource>> restoreVersion(String portalKey, int version) {
        return restoreVersion(portalKey, null, version);
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
