package org.chovy.canvas.web.risk;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionReplayMismatchException;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionService;
import org.chovy.canvas.web.risk.dto.RiskDecisionEvaluateRequest;
import org.chovy.canvas.web.risk.dto.RiskDecisionEvaluateResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * 风控决策 HTTP 边界，提供租户隔离的低延迟在线评估接口。
 */
@RestController
@RequestMapping("/canvas/risk/decisions")
public class RiskDecisionController {

    /**
     * defaultdeadlinems常量，用于保持控制器内部规则一致。
     */
    private static final int DEFAULT_DEADLINE_MS = 50;

    /**
     * 决策服务，用于承接对应业务能力和领域编排。
     */
    private final RiskDecisionService decisionService;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;
    /**
     * 审计sink，用于保存请求处理过程中需要的业务数据。
     */
    private final RiskDecisionAuditSink auditSink;
    /**
     * budget提供方，用于保存请求处理过程中需要的业务数据。
     */
    private final RiskSceneBudgetProvider budgetProvider;
    /**
     * clock，用于保存请求处理过程中需要的业务数据。
     */
    private final Clock clock;
    /**
     * trace读取器，用于保存请求处理过程中需要的业务数据。
     */
    private final RiskDecisionTraceReader traceReader;

    /**
     * 创建默认风控决策控制器，使用空审计和默认场景预算。
     */
    @Autowired
    public RiskDecisionController(RiskDecisionService decisionService,
                                  TenantContextResolver tenantContextResolver,
                                  RiskDecisionTraceReader traceReader) {
        this(decisionService, tenantContextResolver, (tenantId, bodyTenantId, actor) -> {
        }, (tenantId, sceneKey) -> DEFAULT_DEADLINE_MS, Clock.systemUTC(), traceReader);
    }

    /**
     * 创建可注入审计、场景预算和时钟的风控决策控制器。
     */
    public RiskDecisionController(RiskDecisionService decisionService,
                                  TenantContextResolver tenantContextResolver,
                                  RiskDecisionAuditSink auditSink,
                                  RiskSceneBudgetProvider budgetProvider,
                                  Clock clock) {
        this(decisionService, tenantContextResolver, auditSink, budgetProvider, clock,
                (tenantId, sceneKey, limit) -> List.of());
    }

    /**
     * 创建可注入审计、场景预算、时钟和追踪读取器的风控决策控制器。
     */
    public RiskDecisionController(RiskDecisionService decisionService,
                                  TenantContextResolver tenantContextResolver,
                                  RiskDecisionAuditSink auditSink,
                                  RiskSceneBudgetProvider budgetProvider,
                                  Clock clock,
                                  RiskDecisionTraceReader traceReader) {
        this.decisionService = decisionService;
        this.tenantContextResolver = tenantContextResolver;
        this.auditSink = auditSink;
        this.budgetProvider = budgetProvider;
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.traceReader = traceReader == null ? (tenantId, sceneKey, limit) -> List.of() : traceReader;
    }

    /**
     * 查询最近决策追踪，供风控工作台回放和排查使用。
     */
    @GetMapping("/traces")
    public Mono<R<List<RiskDecisionTraceView>>> listTraces(@RequestParam(required = false) String sceneKey,
                                                           @RequestParam(defaultValue = "50") int limit) {
        return currentEvaluator().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(traceReader.listTraces(context.tenantId(), sceneKey, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 接收在线风控评估请求，完成租户鉴权、参数校验、服务调用和 DTO 转换。
     */
    @PostMapping("/evaluate")
    public Mono<R<RiskDecisionEvaluateResponse>> evaluate(@RequestBody RiskDecisionEvaluateRequest body) {
        return currentEvaluator().flatMap(context -> Mono.fromCallable(() -> {
                    RiskDecisionEvaluateRequest request = body == null
                            ? new RiskDecisionEvaluateRequest(null, null, null, Map.of(), null,
                            Map.of(), Map.of(), Map.of(), null)
                            : body;
                    validate(context, request);
                    org.chovy.canvas.domain.risk.runtime.RiskDecisionResponse response =
                            decisionService.evaluate(toRuntimeRequest(context, request));
                    return R.ok(toDto(response));
                })
                .onErrorMap(RiskDecisionReplayMismatchException.class,
                        error -> new ResponseStatusException(HttpStatus.CONFLICT, error.getMessage(), error))
                // 评估会持久化决策并可能访问特征存储，需要离开响应式事件循环执行。
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 读取当前租户上下文，并校验调用方具备风控评估权限。
     */
    private Mono<TenantContext> currentEvaluator() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, RoleNames.ADMIN, "system"));
        }
        // 租户上下文是权限来源，请求体不能提升权限或切换租户。
        return tenantContextResolver.current()
                .switchIfEmpty(Mono.error(new AccessDeniedException("risk decision evaluate permission required")))
                .filter(this::hasEvaluatePermission)
                .switchIfEmpty(Mono.error(new AccessDeniedException("risk decision evaluate permission required")));
    }

    /**
     * 判断当前租户角色是否允许执行在线风控评估。
     */
    private boolean hasEvaluatePermission(TenantContext context) {
        return context.isSuperAdmin()
                || context.isTenantAdmin()
                || RoleNames.OPERATOR.equals(context.role());
    }

    /**
     * 校验请求必填字段、主体标识、事件时间和场景延迟预算。
     */
    private void validate(TenantContext context, RiskDecisionEvaluateRequest request) {
        if (isBlank(request.requestId())) {
            /**
             * 执行 bad请求 对应的内部处理流程。
             *
             * @param required" required"，由调用方提供
             * @return 返回内部处理结果
             */
            throw badRequest("requestId is required");
        }
        if (isBlank(request.sceneKey())) {
            /**
             * 执行 bad请求 对应的内部处理流程。
             *
             * @param required" required"，由调用方提供
             * @return 返回内部处理结果
             */
            throw badRequest("sceneKey is required");
        }
        if (!hasSubjectIdentifier(request.subject())) {
            /**
             * 执行 bad请求 对应的内部处理流程。
             *
             * @param required" required"，由调用方提供
             * @return 返回内部处理结果
             */
            throw badRequest("subject identifier is required");
        }
        Instant eventTime = parseEventTime(request.eventTime());
        if (eventTime.isAfter(clock.instant().plusSeconds(24 * 60 * 60))) {
            /**
             * 执行 bad请求 对应的内部处理流程。
             *
             * @param future" future"，由调用方提供
             * @return 返回内部处理结果
             */
            throw badRequest("eventTime must not be more than 24 hours in the future");
        }
        int deadlineMs = deadlineMs(request);
        int sceneBudget = budgetProvider.latencyBudgetMs(context.tenantId(), request.sceneKey());
        if (deadlineMs > sceneBudget) {
            /**
             * 执行 bad请求 对应的内部处理流程。
             *
             * @param budget" budget"，由调用方提供
             * @return 返回内部处理结果
             */
            throw badRequest("deadline must not exceed scene latency budget");
        }
        if (request.tenantId() != null && !request.tenantId().equals(context.tenantId())) {
            // 请求体租户编号仅作为旧客户端提示；不一致时记录审计并忽略。
            auditSink.tenantOverrideIgnored(context.tenantId(), request.tenantId(), actor(context));
        }
    }

    /**
     * 将 HTTP 请求 DTO 转换为运行时风控请求。
     */
    private org.chovy.canvas.domain.risk.runtime.RiskDecisionRequest toRuntimeRequest(
            TenantContext context,
            RiskDecisionEvaluateRequest request) {
        return new org.chovy.canvas.domain.risk.runtime.RiskDecisionRequest(
                context.tenantId(),
                request.requestId(),
                request.sceneKey(),
                parseEventTime(request.eventTime()),
                request.event(),
                request.subject(),
                request.context(),
                request.features(),
                deadlineMs(request));
    }

    /**
     * 将领域决策响应转换为接口响应 DTO。
     */
    private RiskDecisionEvaluateResponse toDto(org.chovy.canvas.domain.risk.runtime.RiskDecisionResponse response) {
        return new RiskDecisionEvaluateResponse(
                response.requestId(),
                response.decisionRunId(),
                response.sceneKey(),
                response.strategyKey(),
                response.strategyVersion(),
                response.mode() == null ? null : response.mode().name(),
                response.action().name(),
                response.score(),
                response.riskBand().name(),
                response.reasons(),
                response.matchedRules(),
                response.labels(),
                response.missingFeatures(),
                response.traceAvailable(),
                response.latencyMs());
    }

    /**
     * 读取请求级超时时间，未指定时使用默认风控接口期限。
     */
    private int deadlineMs(RiskDecisionEvaluateRequest request) {
        if (request.options() == null || request.options().deadlineMs() == null) {
            return DEFAULT_DEADLINE_MS;
        }
        return request.options().deadlineMs();
    }

    /**
     * 解析 ISO-8601 事件时间。
     */
    private Instant parseEventTime(String eventTime) {
        if (isBlank(eventTime)) {
            /**
             * 执行 bad请求 对应的内部处理流程。
             *
             * @param required" required"，由调用方提供
             * @return 返回内部处理结果
             */
            throw badRequest("eventTime is required");
        }
        try {
            return Instant.parse(eventTime);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (DateTimeParseException error) {
            /**
             * 执行 bad请求 对应的内部处理流程。
             *
             * @param ISO-8601" iso-8601"，由调用方提供
             * @return 返回内部处理结果
             */
            throw badRequest("eventTime must be ISO-8601");
        }
    }

    /**
     * 判断主体对象是否至少包含一种可用于风控识别的标识。
     */
    private boolean hasSubjectIdentifier(Map<String, Object> subject) {
        return hasText(subject, "userId")
                || hasText(subject, "deviceId")
                || hasText(subject, "ip")
                || hasText(subject, "email")
                || hasText(subject, "phone");
    }

    /**
     * 判断 Map 中指定键是否存在非空文本值。
     */
    private boolean hasText(Map<String, Object> source, String key) {
        Object value = source == null ? null : source.get(key);
        return value != null && !value.toString().isBlank();
    }

    /**
     * 从租户上下文获取审计操作者名称。
     */
    private String actor(TenantContext context) {
        return context.username() == null ? "system" : context.username();
    }

    /**
     * 判断字符串是否为空。
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 构造 400 请求错误。
     */
    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
