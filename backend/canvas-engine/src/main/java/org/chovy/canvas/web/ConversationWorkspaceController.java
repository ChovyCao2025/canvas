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

@RestController
@RequestMapping("/canvas/conversations/workspace")
public class ConversationWorkspaceController {

    private final ConversationWorkspaceService service;
    private final ConversationRoutingService routingService;
    private final ConversationAiReplyService aiReplyService;
    private final TenantContextResolver tenantContextResolver;

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

    @PostMapping("/sessions/{sessionId}/work-item")
    public Mono<R<ConversationWorkItemView>> ensureWorkItem(@PathVariable Long sessionId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.ensureWorkItemForSession(tenantId(context), sessionId, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

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

    @PostMapping("/work-items/{workItemId}/assign")
    public Mono<R<ConversationWorkItemView>> assign(@PathVariable Long workItemId,
                                                    @RequestBody ConversationAssignmentCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.assign(tenantId(context), workItemId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/work-items/{workItemId}/status")
    public Mono<R<ConversationWorkItemView>> updateStatus(@PathVariable Long workItemId,
                                                          @RequestBody ConversationWorkItemStatusCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.updateStatus(tenantId(context), workItemId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/work-items/{workItemId}/tasks")
    public Mono<R<ConversationSopTaskView>> createTask(@PathVariable Long workItemId,
                                                       @RequestBody ConversationSopTaskCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.createTask(tenantId(context), workItemId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/tasks/{taskId}/complete")
    public Mono<R<ConversationSopTaskView>> completeTask(@PathVariable Long taskId,
                                                         @RequestBody ConversationSopTaskCompletionCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.completeTask(tenantId(context), taskId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

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

    @PostMapping("/routing-agents")
    public Mono<R<ConversationRoutingAgentView>> upsertRoutingAgent(
            @RequestBody ConversationRoutingAgentCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(routingService.upsertAgent(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/routing-rules")
    public Mono<R<ConversationRoutingRuleView>> upsertRoutingRule(
            @RequestBody ConversationRoutingRuleCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(routingService.upsertRule(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/work-items/{workItemId}/route")
    public Mono<R<ConversationRouteResultView>> routeWorkItem(
            @PathVariable Long workItemId,
            @RequestBody ConversationRouteCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(routingService.routeWorkItem(tenantId(context), workItemId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

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

    @GetMapping("/sla-breaches")
    public Mono<R<List<ConversationSlaBreachView>>> slaBreaches(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(routingService.slaBreaches(tenantId(context), status, boundedLimit(limit))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/work-items/{workItemId}/ai-reply-suggestions/generate")
    public Mono<R<ConversationAiReplySuggestionView>> generateAiReplySuggestion(
            @PathVariable Long workItemId,
            @RequestBody ConversationAiReplyGenerateCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(aiReplyService.generate(tenantId(context), workItemId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/work-items/{workItemId}/ai-reply-suggestions/{suggestionId}/review")
    public Mono<R<ConversationAiReplySuggestionView>> reviewAiReplySuggestion(
            @PathVariable Long workItemId,
            @PathVariable Long suggestionId,
            @RequestBody ConversationAiReplyReviewCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(aiReplyService.review(tenantId(context), workItemId, suggestionId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

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

    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.currentOrError();
    }

    private Long tenantId(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }

    private String actor(TenantContext context) {
        return context == null || context.username() == null || context.username().isBlank()
                ? "system"
                : context.username();
    }

    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }
}
