package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.conversation.ConversationAiReplyGenerateCommand;
import org.chovy.canvas.domain.conversation.ConversationAiReplyReviewCommand;
import org.chovy.canvas.domain.conversation.ConversationAiReplyService;
import org.chovy.canvas.domain.conversation.ConversationAiReplySuggestionQuery;
import org.chovy.canvas.domain.conversation.ConversationAiReplySuggestionView;
import org.chovy.canvas.domain.conversation.ConversationAssignmentCommand;
import org.chovy.canvas.domain.conversation.ConversationInboxQuery;
import org.chovy.canvas.domain.conversation.ConversationRouteCommand;
import org.chovy.canvas.domain.conversation.ConversationRouteResultView;
import org.chovy.canvas.domain.conversation.ConversationRoutingAgentCommand;
import org.chovy.canvas.domain.conversation.ConversationRoutingAgentView;
import org.chovy.canvas.domain.conversation.ConversationRoutingRuleCommand;
import org.chovy.canvas.domain.conversation.ConversationRoutingRuleView;
import org.chovy.canvas.domain.conversation.ConversationRoutingService;
import org.chovy.canvas.domain.conversation.ConversationSlaBreachView;
import org.chovy.canvas.domain.conversation.ConversationSlaEvaluationView;
import org.chovy.canvas.domain.conversation.ConversationSopTaskCommand;
import org.chovy.canvas.domain.conversation.ConversationSopTaskCompletionCommand;
import org.chovy.canvas.domain.conversation.ConversationSopTaskView;
import org.chovy.canvas.domain.conversation.ConversationWorkItemStatusCommand;
import org.chovy.canvas.domain.conversation.ConversationWorkItemView;
import org.chovy.canvas.domain.conversation.ConversationWorkspaceService;
import org.chovy.canvas.domain.conversation.ConversationWorkspaceTimelineView;
import org.springframework.beans.factory.annotation.Autowired;
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
 * ConversationWorkspaceController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/conversations/workspace")
public class ConversationWorkspaceController {

    /**
     * 服务，用于承接对应业务能力和领域编排。
     */
    private final ConversationWorkspaceService service;
    /**
     * routing服务，用于承接对应业务能力和领域编排。
     */
    private final ConversationRoutingService routingService;
    /**
     * aireply服务，用于承接对应业务能力和领域编排。
     */
    private final ConversationAiReplyService aiReplyService;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 ConversationWorkspaceController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param routingService 依赖组件，用于完成数据访问或外部能力调用。
     * @param aiReplyService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public ConversationWorkspaceController(ConversationWorkspaceService service,
                                           ConversationRoutingService routingService,
                                           ConversationAiReplyService aiReplyService,
                                           TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.routingService = routingService;
        this.aiReplyService = aiReplyService;
        this.tenantContextResolver = tenantContextResolver;
    }

    ConversationWorkspaceController(ConversationWorkspaceService service,
                                    ConversationRoutingService routingService,
                                    TenantContextResolver tenantContextResolver) {
        this(service, routingService, null, tenantContextResolver);
    }
    /**
     * 处理 会话 Workspace 请求接口，对应 POST /sessions/{sessionId}/work-item。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param sessionId session ID。
     * @return 异步返回统一响应，包含处理 会话 Workspace 请求后的业务数据。
     */
    @PostMapping("/sessions/{sessionId}/work-item")
    public Mono<R<ConversationWorkItemView>> ensureWorkItem(@PathVariable Long sessionId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.ensureWorkItemForSession(tenantId(context), sessionId, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 会话 Workspace 请求接口，对应 GET /inbox。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param status 状态过滤条件，可选。
     * @param assignedTo 请求参数，可选。
     * @param channel 渠道过滤条件，可选。
     * @param limit 返回数量上限，默认值为 50。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/inbox")
    public Mono<R<List<ConversationWorkItemView>>> inbox(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(required = false) String channel,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.inbox(tenantId(context),
                                new ConversationInboxQuery(status, assignedTo, channel, boundedLimit(limit)))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 会话 Workspace 请求接口，对应 POST /work-items/{workItemId}/assign。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param workItemId work Item ID。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含处理 会话 Workspace 请求后的业务数据。
     */
    @PostMapping("/work-items/{workItemId}/assign")
    public Mono<R<ConversationWorkItemView>> assign(@PathVariable Long workItemId,
                                                    @RequestBody ConversationAssignmentCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.assign(tenantId(context), workItemId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 更新 会话 Workspace接口，对应 POST /work-items/{workItemId}/status。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用：会修改已有记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param workItemId work Item ID。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含更新 会话 Workspace后的业务数据。
     */
    @PostMapping("/work-items/{workItemId}/status")
    public Mono<R<ConversationWorkItemView>> updateStatus(@PathVariable Long workItemId,
                                                          @RequestBody ConversationWorkItemStatusCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.updateStatus(tenantId(context), workItemId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建 会话 Workspace接口，对应 POST /work-items/{workItemId}/tasks。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用：会写入新记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param workItemId work Item ID。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建 会话 Workspace后的业务数据。
     */
    @PostMapping("/work-items/{workItemId}/tasks")
    public Mono<R<ConversationSopTaskView>> createTask(@PathVariable Long workItemId,
                                                       @RequestBody ConversationSopTaskCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.createTask(tenantId(context), workItemId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 发起 会话 Workspace 智能问数接口，对应 POST /tasks/{taskId}/complete。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param taskId task ID。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含发起 会话 Workspace 智能问数后的业务数据。
     */
    @PostMapping("/tasks/{taskId}/complete")
    public Mono<R<ConversationSopTaskView>> completeTask(@PathVariable Long taskId,
                                                         @RequestBody ConversationSopTaskCompletionCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.completeTask(tenantId(context), taskId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 会话 Workspace 请求接口，对应 GET /work-items/{workItemId}/timeline。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param workItemId work Item ID。
     * @param messageLimit 请求参数，默认值为 50。
     * @param auditLimit 请求参数，默认值为 50。
     * @return 异步返回统一响应，包含处理 会话 Workspace 请求后的业务数据。
     */
    @GetMapping("/work-items/{workItemId}/timeline")
    public Mono<R<ConversationWorkspaceTimelineView>> timeline(
            @PathVariable Long workItemId,
            @RequestParam(defaultValue = "50") int messageLimit,
            @RequestParam(defaultValue = "50") int auditLimit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.timeline(
                                tenantId(context),
                                workItemId,
                                boundedLimit(messageLimit),
                                boundedLimit(auditLimit))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建或更新 会话 Workspace接口，对应 POST /routing-agents。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 routingService.upsertAgent 完成业务处理。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建或更新 会话 Workspace后的业务数据。
     */
    @PostMapping("/routing-agents")
    public Mono<R<ConversationRoutingAgentView>> upsertRoutingAgent(
            @RequestBody ConversationRoutingAgentCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(routingService.upsertAgent(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建或更新 会话 Workspace接口，对应 POST /routing-rules。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 routingService.upsertRule 完成业务处理。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建或更新 会话 Workspace后的业务数据。
     */
    @PostMapping("/routing-rules")
    public Mono<R<ConversationRoutingRuleView>> upsertRoutingRule(
            @RequestBody ConversationRoutingRuleCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(routingService.upsertRule(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 会话 Workspace 请求接口，对应 POST /work-items/{workItemId}/route。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 routingService.routeWorkItem 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param workItemId work Item ID。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含处理 会话 Workspace 请求后的业务数据。
     */
    @PostMapping("/work-items/{workItemId}/route")
    public Mono<R<ConversationRouteResultView>> routeWorkItem(
            @PathVariable Long workItemId,
            @RequestBody ConversationRouteCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(routingService.routeWorkItem(tenantId(context), workItemId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 评估 会话 Workspace接口，对应 POST /sla-breaches/evaluate。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 routingService.evaluateSlaBreaches 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param limit 返回数量上限，默认值为 50。
     * @return 异步返回统一响应，包含评估 会话 Workspace后的业务数据。
     */
    @PostMapping("/sla-breaches/evaluate")
    public Mono<R<ConversationSlaEvaluationView>> evaluateSlaBreaches(
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(routingService.evaluateSlaBreaches(
                                tenantId(context),
                                null,
                                actor(context),
                                boundedLimit(limit))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 会话 Workspace 请求接口，对应 GET /sla-breaches。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 routingService.slaBreaches 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param status 状态过滤条件，可选。
     * @param limit 返回数量上限，默认值为 50。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/sla-breaches")
    public Mono<R<List<ConversationSlaBreachView>>> slaBreaches(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(routingService.slaBreaches(tenantId(context), status, boundedLimit(limit))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 会话 Workspace 请求接口，对应 POST /work-items/{workItemId}/ai-reply-suggestions/generate。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 aiReplyService.generate 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param workItemId work Item ID。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含处理 会话 Workspace 请求后的业务数据。
     */
    @PostMapping("/work-items/{workItemId}/ai-reply-suggestions/generate")
    public Mono<R<ConversationAiReplySuggestionView>> generateAiReplySuggestion(
            @PathVariable Long workItemId,
            @RequestBody ConversationAiReplyGenerateCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(aiReplyService.generate(tenantId(context), workItemId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 会话 Workspace 请求接口，对应 POST /work-items/{workItemId}/ai-reply-suggestions/{suggestionId}/review。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 aiReplyService.review 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param workItemId work Item ID。
     * @param suggestionId suggestion ID。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含处理 会话 Workspace 请求后的业务数据。
     */
    @PostMapping("/work-items/{workItemId}/ai-reply-suggestions/{suggestionId}/review")
    public Mono<R<ConversationAiReplySuggestionView>> reviewAiReplySuggestion(
            @PathVariable Long workItemId,
            @PathVariable Long suggestionId,
            @RequestBody ConversationAiReplyReviewCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(aiReplyService.review(tenantId(context), workItemId, suggestionId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 会话 Workspace 请求接口，对应 GET /work-items/{workItemId}/ai-reply-suggestions。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 aiReplyService.list 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param workItemId work Item ID。
     * @param status 状态过滤条件，可选。
     * @param limit 返回数量上限，默认值为 50。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/work-items/{workItemId}/ai-reply-suggestions")
    public Mono<R<List<ConversationAiReplySuggestionView>>> aiReplySuggestions(
            @PathVariable Long workItemId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(aiReplyService.list(
                                tenantId(context),
                                workItemId,
                                new ConversationAiReplySuggestionQuery(status, boundedLimit(limit)))))
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
