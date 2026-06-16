package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.ai.BiAskDataAgentService;
import org.chovy.canvas.domain.bi.ai.BiAskDataRequest;
import org.chovy.canvas.domain.bi.ai.BiAskDataResponse;
import org.chovy.canvas.domain.bi.ai.BiDashboardDraftAgentService;
import org.chovy.canvas.domain.bi.ai.BiDashboardDraftRequest;
import org.chovy.canvas.domain.bi.ai.BiDashboardDraftResponse;
import org.chovy.canvas.domain.bi.ai.BiInsightAgentService;
import org.chovy.canvas.domain.bi.ai.BiInsightRequest;
import org.chovy.canvas.domain.bi.ai.BiInsightResponse;
import org.chovy.canvas.domain.bi.ai.BiInterpretationAgentService;
import org.chovy.canvas.domain.bi.ai.BiInterpretationRequest;
import org.chovy.canvas.domain.bi.ai.BiInterpretationResponse;
import org.chovy.canvas.domain.bi.ai.BiReportAgentService;
import org.chovy.canvas.domain.bi.ai.BiReportRequest;
import org.chovy.canvas.domain.bi.ai.BiReportResponse;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * BiAiController 暴露 web.bi 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/bi/ai")
public class BiAiController {

    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;
    /**
     * askdataagent服务，用于承接对应业务能力和领域编排。
     */
    private final BiAskDataAgentService askDataAgentService;
    /**
     * interpretationagent服务，用于承接对应业务能力和领域编排。
     */
    private final BiInterpretationAgentService interpretationAgentService;
    /**
     * reportagent服务，用于承接对应业务能力和领域编排。
     */
    private final BiReportAgentService reportAgentService;
    /**
     * 仪表盘draftagent服务，用于承接对应业务能力和领域编排。
     */
    private final BiDashboardDraftAgentService dashboardDraftAgentService;
    /**
     * insightagent服务，用于承接对应业务能力和领域编排。
     */
    private final BiInsightAgentService insightAgentService;

    /**
     * 创建 BiAiController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param askDataAgentService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiAiController(TenantContextResolver tenantContextResolver,
                          BiAskDataAgentService askDataAgentService) {
        this(tenantContextResolver, askDataAgentService, null, null, null, null);
    }

    /**
     * 创建 BiAiController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param askDataAgentService 依赖组件，用于完成数据访问或外部能力调用。
     * @param interpretationAgentService 依赖组件，用于完成数据访问或外部能力调用。
     * @param reportAgentService 依赖组件，用于完成数据访问或外部能力调用。
     * @param dashboardDraftAgentService 依赖组件，用于完成数据访问或外部能力调用。
     * @param insightAgentService 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public BiAiController(TenantContextResolver tenantContextResolver,
                          BiAskDataAgentService askDataAgentService,
                          BiInterpretationAgentService interpretationAgentService,
                          BiReportAgentService reportAgentService,
                          BiDashboardDraftAgentService dashboardDraftAgentService,
                          BiInsightAgentService insightAgentService) {
        this.tenantContextResolver = tenantContextResolver;
        this.askDataAgentService = askDataAgentService;
        this.interpretationAgentService = interpretationAgentService;
        this.reportAgentService = reportAgentService;
        this.dashboardDraftAgentService = dashboardDraftAgentService;
        this.insightAgentService = insightAgentService;
    }
    /**
     * 发起 BI AI 智能问数接口，对应 POST /ask。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 askDataAgentService.ask 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param request 请求体。
     * @return 异步返回统一响应，包含发起 BI AI 智能问数后的业务数据。
     */
    @PostMapping("/ask")
    public Mono<R<BiAskDataResponse>> askData(@RequestBody BiAskDataRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(askDataAgentService.ask(request, new BiQueryContext(
                                normalizeTenant(context),
                                context == null ? "system" : context.username(),
                                context == null ? null : context.role()))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 生成 BI AI 解读接口，对应 POST /interpret。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 interpretationAgentService.interpret 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param request 请求体。
     * @return 异步返回统一响应，包含生成 BI AI 解读后的业务数据。
     */
    @PostMapping("/interpret")
    public Mono<R<BiInterpretationResponse>> interpret(@RequestBody BiInterpretationRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(interpretationAgentService.interpret(request, queryContext(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 生成 BI AI 报告接口，对应 POST /report。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 reportAgentService.generate 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param request 请求体。
     * @return 异步返回统一响应，包含生成 BI AI 报告后的业务数据。
     */
    @PostMapping("/report")
    public Mono<R<BiReportResponse>> report(@RequestBody BiReportRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(reportAgentService.generate(request, queryContext(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 BI AI 请求接口，对应 POST /dashboard-draft。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 dashboardDraftAgentService.generate 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param request 请求体。
     * @return 异步返回统一响应，包含处理 BI AI 请求后的业务数据。
     */
    @PostMapping("/dashboard-draft")
    public Mono<R<BiDashboardDraftResponse>> dashboardDraft(@RequestBody BiDashboardDraftRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(dashboardDraftAgentService.generate(request, queryContext(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 生成 BI AI 洞察接口，对应 POST /insights。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 insightAgentService.inspect 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param request 请求体。
     * @return 异步返回统一响应，包含生成 BI AI 洞察后的业务数据。
     */
    @PostMapping("/insights")
    public Mono<R<BiInsightResponse>> insights(@RequestBody BiInsightRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(insightAgentService.inspect(request, queryContext(context))))
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
     * 解析并规范化租户 ID。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }

    /**
     * 查询或读取业务数据。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回符合条件的数据列表或视图。
     */
    private BiQueryContext queryContext(TenantContext context) {
        return new BiQueryContext(
                normalizeTenant(context),
                context == null ? "system" : context.username(),
                context == null ? null : context.role());
    }
}
