package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.ai.AiDecisionFeedbackCommand;
import org.chovy.canvas.domain.ai.AiDecisionFeedbackView;
import org.chovy.canvas.domain.ai.AiDecisionModelService;
import org.chovy.canvas.domain.ai.AiDecisionRecommendationQuery;
import org.chovy.canvas.domain.ai.AiDecisionRecommendationView;
import org.chovy.canvas.domain.ai.AiDecisionRecomputeCommand;
import org.chovy.canvas.domain.ai.AiDecisionRunView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
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
 * AiDecisionController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/ai/decisions")
public class AiDecisionController {

    /**
     * 决策model服务，用于承接对应业务能力和领域编排。
     */
    private final AiDecisionModelService decisionModelService;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 AiDecisionController 实例并注入 web 场景依赖。
     * @param decisionModelService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public AiDecisionController(AiDecisionModelService decisionModelService) {
        this(decisionModelService, null);
    }

    /**
     * 创建 AiDecisionController 实例并注入 web 场景依赖。
     * @param decisionModelService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public AiDecisionController(AiDecisionModelService decisionModelService,
                                TenantContextResolver tenantContextResolver) {
        this.decisionModelService = decisionModelService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 重新计算 AI 决策接口，对应 POST /recompute。
     * 接口在控制器或服务层执行资源权限校验后再处理请求。
     * 主要委托 decisionModelService.recompute 完成业务处理。
     * 副作用：会触发重新计算。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体，可选。
     * @return 异步返回统一响应，包含重新计算 AI 决策后的业务数据。
     */
    @PostMapping("/recompute")
    public Mono<R<AiDecisionRunView>> recompute(@RequestBody(required = false) AiDecisionRecomputeCommand command) {
        return currentAdmin().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(decisionModelService.recompute(tenantId(context), command, username(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 触发 AI 决策运行接口，对应 GET /latest-run。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 主要委托 decisionModelService.latestRun 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param decisionScope 请求参数，默认值为 DAILY_MARKETING。
     * @return 异步返回统一响应，包含触发 AI 决策运行后的业务数据。
     */
    @GetMapping("/latest-run")
    public Mono<R<AiDecisionRunView>> latestRun(@RequestParam(defaultValue = "DAILY_MARKETING") String decisionScope) {
        return currentAdmin().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(decisionModelService.latestRun(tenantId(context), decisionScope).orElse(null)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 AI 决策 请求接口，对应 GET /recommendations。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 主要委托 decisionModelService.recommendations 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param runId run ID，可选。
     * @param decisionType 请求参数，可选。
     * @param eligibilityStatus 请求参数，可选。
     * @param limit 返回数量上限，默认值为 100。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/recommendations")
    public Mono<R<List<AiDecisionRecommendationView>>> recommendations(
            @RequestParam(required = false) Long runId,
            @RequestParam(required = false) String decisionType,
            @RequestParam(required = false) String eligibilityStatus,
            @RequestParam(defaultValue = "100") int limit) {
        return currentAdmin().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(decisionModelService.recommendations(
                                tenantId(context),
                                new AiDecisionRecommendationQuery(runId, decisionType, eligibilityStatus, boundedLimit(limit)))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 记录AI 决策数据接口，对应 POST /recommendations/{recommendationId}/feedback。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 主要委托 decisionModelService.recordFeedback 完成业务处理。
     * 副作用：会记录业务快照。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param recommendationId 推荐 ID。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含记录AI 决策数据后的业务数据。
     */
    @PostMapping("/recommendations/{recommendationId}/feedback")
    public Mono<R<AiDecisionFeedbackView>> recordFeedback(@PathVariable Long recommendationId,
                                                          @RequestBody AiDecisionFeedbackCommand command) {
        return currentAdmin().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(decisionModelService.recordFeedback(
                                tenantId(context), recommendationId, command, username(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 currentAdmin 流程生成的业务结果。
     */
    private Mono<TenantContext> currentAdmin() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, "ADMIN", "system"));
        }
        return tenantContextResolver.current()
                .switchIfEmpty(Mono.error(new AccessDeniedException("AI decision requires admin access")))
                .filter(context -> context.isSuperAdmin() || context.isTenantAdmin())
                .switchIfEmpty(Mono.error(new AccessDeniedException("AI decision requires admin access")));
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
     * @return 返回 username 生成的文本或业务键。
     */
    private String username(TenantContext context) {
        return context == null || context.username() == null ? "system" : context.username();
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
