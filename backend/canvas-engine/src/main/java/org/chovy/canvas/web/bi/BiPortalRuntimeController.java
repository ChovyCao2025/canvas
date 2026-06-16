package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.portal.BiPortalResource;
import org.chovy.canvas.domain.bi.portal.BiPortalRuntimeService;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * BiPortalRuntimeController 暴露 web.bi 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/bi/portals/runtime")
public class BiPortalRuntimeController {

    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;
    /**
     * 门户运行态服务，用于承接对应业务能力和领域编排。
     */
    private final BiPortalRuntimeService portalRuntimeService;

    /**
     * 创建 BiPortalRuntimeController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param portalRuntimeService 时间参数，用于计算窗口、过期或审计时间。
     */
    public BiPortalRuntimeController(TenantContextResolver tenantContextResolver,
                                     BiPortalRuntimeService portalRuntimeService) {
        this.tenantContextResolver = tenantContextResolver;
        this.portalRuntimeService = portalRuntimeService;
    }
    /**
     * 查询 BI 门户运行态列表接口，对应 GET 请求。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 portalRuntimeService.listPublished 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping
    public Mono<R<List<BiPortalResource>>> list() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(portalRuntimeService.listPublished(
                                context.tenantId(),
                                queryContext(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 获取 BI 门户运行态详情接口，对应 GET /{portalKey}。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 portalRuntimeService.getPublished 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param portalKey 门户唯一键。
     * @return 异步返回统一响应，包含获取 BI 门户运行态详情后的业务数据。
     */
    @GetMapping("/{portalKey}")
    public Mono<R<BiPortalResource>> get(@PathVariable String portalKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(portalRuntimeService.getPublished(
                                context.tenantId(),
                                portalKey,
                                queryContext(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 查询或读取业务数据。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回符合条件的数据列表或视图。
     */
    private BiQueryContext queryContext(TenantContext context) {
        return new BiQueryContext(context.tenantId(), context.username(), context.role());
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
