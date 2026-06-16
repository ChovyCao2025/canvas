package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyDetectionCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyDetectionService;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyDetectionView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyEventQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyEventView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyRuleCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyRuleView;
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
 * MarketingMonitorAnomalyController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/marketing-monitoring")
public class MarketingMonitorAnomalyController {

    /**
     * 服务，用于承接对应业务能力和领域编排。
     */
    private final MarketingMonitorAnomalyDetectionService service;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 MarketingMonitorAnomalyController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public MarketingMonitorAnomalyController(MarketingMonitorAnomalyDetectionService service,
                                             TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 创建或更新 营销 Monitor Anomaly接口，对应 POST /anomaly-rules。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建或更新 营销 Monitor Anomaly后的业务数据。
     */
    @PostMapping("/anomaly-rules")
    public Mono<R<MarketingMonitorAnomalyRuleView>> upsertRule(
            @RequestBody MarketingMonitorAnomalyRuleCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertRule(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 营销 Monitor Anomaly 请求接口，对应 POST /anomalies/detect。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含处理 营销 Monitor Anomaly 请求后的业务数据。
     */
    @PostMapping("/anomalies/detect")
    public Mono<R<MarketingMonitorAnomalyDetectionView>> detect(
            @RequestBody MarketingMonitorAnomalyDetectionCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.detect(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 营销 Monitor Anomaly 请求接口，对应 GET /anomalies。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param ruleId rule ID，可选。
     * @param status 状态过滤条件，可选。
     * @param limit 返回数量上限，默认值为 50。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/anomalies")
    public Mono<R<List<MarketingMonitorAnomalyEventView>>> events(
            @RequestParam(required = false) Long ruleId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.events(tenantId(context),
                                new MarketingMonitorAnomalyEventQuery(ruleId, status, boundedLimit(limit)))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 营销 Monitor Anomaly 请求接口，对应 POST /anomalies/{eventId}/resolve。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param eventId event ID。
     * @return 异步返回统一响应，包含处理 营销 Monitor Anomaly 请求后的业务数据。
     */
    @PostMapping("/anomalies/{eventId}/resolve")
    public Mono<R<MarketingMonitorAnomalyEventView>> resolveEvent(@PathVariable Long eventId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.resolveEvent(tenantId(context), eventId, actor(context))))
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

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }
}
