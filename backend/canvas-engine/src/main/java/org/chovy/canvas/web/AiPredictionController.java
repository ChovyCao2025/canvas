package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.ai.ChurnPredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/ai/predictions")
/**
 * AiPredictionController 提供对应业务域的 HTTP 接口入口。
 */
public class AiPredictionController {

    /**
     * churnprediction服务，用于承接对应业务能力和领域编排。
     */
    private final ChurnPredictionService churnPredictionService;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 使用预测服务创建控制器，兼容未接入租户解析器的旧测试场景。
     *
     * @param churnPredictionService 流失预测服务
     */
    public AiPredictionController(ChurnPredictionService churnPredictionService) {
        this(churnPredictionService, null);
    }

    @Autowired
    /**
     * 使用预测服务和租户解析器创建控制器。
     *
     * @param churnPredictionService 流失预测服务
     * @param tenantContextResolver 租户上下文解析器
     */
    public AiPredictionController(ChurnPredictionService churnPredictionService,
                                  TenantContextResolver tenantContextResolver) {
        this.churnPredictionService = churnPredictionService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping("/latest-run")
    /**
     * 查询并返回对应资源或视图。
     * @return 返回处理后的业务结果
     */
    public Mono<R<ChurnPredictionService.PredictionRunView>> latestRun() {
        return currentAdmin().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(churnPredictionService.latestRun(tenantId(context)).orElse(null)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/readiness")
    /**
     * 查询并返回对应资源或视图。
     * @return 返回处理后的业务结果
     */
    public Mono<R<ChurnPredictionService.PredictionReadinessView>> readiness() {
        return currentAdmin().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(churnPredictionService.readiness(tenantId(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/churn-distribution")
    /**
     * 查询并返回对应资源或视图。
     * @return 返回处理后的业务结果
     */
    public Mono<R<List<ChurnPredictionService.RiskDistributionItem>>> distribution() {
        return currentAdmin().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(churnPredictionService.churnDistribution(tenantId(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/top-risk-users")
    /**
     * 查询并返回对应资源或视图。
     *
     * @param limit 限制，由调用方或当前请求上下文提供
     * @return 返回处理后的业务结果
     */
    public Mono<R<List<ChurnPredictionService.TopRiskUser>>> topRiskUsers(@RequestParam(defaultValue = "100") int limit) {
        return currentAdmin().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(churnPredictionService.topRiskUsers(tenantId(context), limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/recompute")
    public Mono<R<ChurnPredictionService.PredictionRunView>> recompute(
            @RequestBody(required = false) ChurnPredictionService.RecomputeRequest request) {
        return currentAdmin().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(churnPredictionService.recompute(tenantId(context),
                                request == null ? new ChurnPredictionService.RecomputeRequest(false, null, null) : request)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 解析并校验当前请求上下文。
     * @return 返回处理后的业务结果
     */
    private Mono<TenantContext> currentAdmin() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, "ADMIN", "test"));
        }
        return tenantContextResolver.current()
                .switchIfEmpty(Mono.error(new AccessDeniedException("AI prediction requires admin access")))
                .filter(context -> context.isSuperAdmin() || context.isTenantAdmin())
                .switchIfEmpty(Mono.error(new AccessDeniedException("AI prediction requires admin access")));
    }

    /**
     * 执行 tenantId 对应的接口或辅助流程。
     *
     * @param context 上下文，由调用方或当前请求上下文提供
     * @return 返回处理后的业务结果
     */
    private Long tenantId(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }
}
