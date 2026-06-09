package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.monitoring.MarketingMonitorWebhookIngestionService;
import org.chovy.canvas.domain.monitoring.MarketingMonitorWebhookSecretView;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * MarketingMonitoringWebhookAdminController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/marketing-monitoring")
public class MarketingMonitoringWebhookAdminController {

    private final MarketingMonitorWebhookIngestionService service;
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 MarketingMonitoringWebhookAdminController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public MarketingMonitoringWebhookAdminController(MarketingMonitorWebhookIngestionService service,
                                                    TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 处理 营销 Monitoring Webhook Admin 请求接口，对应 POST /sources/{sourceId}/webhook-secret/rotate。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param sourceId source ID。
     * @return 异步返回统一响应，包含处理 营销 Monitoring Webhook Admin 请求后的业务数据。
     */
    @PostMapping("/sources/{sourceId}/webhook-secret/rotate")
    public Mono<R<MarketingMonitorWebhookSecretView>> rotateSecret(@PathVariable Long sourceId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.rotateSecret(tenantId(context), sourceId, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 currentTenant 流程生成的业务结果。
     */
    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.currentOrError();
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

    /**
     * 解析操作人标识。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 actor 生成的文本或业务键。
     */
    private String actor(TenantContext context) {
        return context == null || context.username() == null || context.username().isBlank()
                ? "system"
                : context.username();
    }
}
