package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.platform.MarketingPlatformControlPlaneService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * MarketingPlatformControlPlaneController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/marketing-platform")
public class MarketingPlatformControlPlaneController {

    private final MarketingPlatformControlPlaneService service;
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 MarketingPlatformControlPlaneController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public MarketingPlatformControlPlaneController(MarketingPlatformControlPlaneService service,
                                                   TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 处理 营销 Platform Control Plane 请求接口，对应 GET /control-plane。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @return 异步返回统一响应，包含处理 营销 Platform Control Plane 请求后的业务数据。
     */
    @GetMapping("/control-plane")
    public Mono<R<MarketingPlatformControlPlaneService.ControlPlaneSummary>> controlPlane() {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.summary(tenantId(context))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private Long tenantId(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }
}
