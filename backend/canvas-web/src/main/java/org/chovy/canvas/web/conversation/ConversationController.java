package org.chovy.canvas.web.conversation;

import java.time.LocalDateTime;
import java.util.Map;

import org.chovy.canvas.conversation.api.ConversationAssignmentCommand;
import org.chovy.canvas.conversation.api.ConversationFacade;
import org.chovy.canvas.conversation.api.ConversationInboundCommand;
import org.chovy.canvas.conversation.api.ConversationRecordResult;
import org.chovy.canvas.conversation.api.ConversationRouteCommand;
import org.chovy.canvas.conversation.api.ConversationRouteResultView;
import org.chovy.canvas.conversation.api.ConversationRoutingAgentCommand;
import org.chovy.canvas.conversation.api.ConversationRoutingAgentView;
import org.chovy.canvas.conversation.api.ConversationRoutingRuleCommand;
import org.chovy.canvas.conversation.api.ConversationRoutingRuleView;
import org.chovy.canvas.conversation.api.ConversationWorkItemStatusCommand;
import org.chovy.canvas.conversation.api.ConversationWorkItemView;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/canvas/conversations")
public class ConversationController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final ConversationFacade facade;

    public ConversationController(ConversationFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/ingress")
    public Mono<CompatibilityEnvelope<ConversationRecordResult>> ingress(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody ConversationIngressRequest request) {
        return envelope(() -> facade.recordInbound(request.toCommand(tenantIdOrDefault(tenantId))));
    }

    @PostMapping("/adapters/{adapterKey}/ingress")
    public Mono<CompatibilityEnvelope<Object>> adapterIngress(
            @PathVariable String adapterKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.recordAdapterInbound(
                tenantIdOrDefault(tenantId),
                adapterKey,
                payload,
                actorOrDefault(actor)));
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<Object>> listSessions(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String channel,
            @RequestParam(defaultValue = "50") int limit) {
        return envelope(() -> facade.listSessions(tenantIdOrDefault(tenantId), userId, channel, limit));
    }

    @GetMapping("/{sessionId}/messages")
    public Mono<CompatibilityEnvelope<Object>> listMessages(
            @PathVariable Long sessionId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "50") int limit) {
        return envelope(() -> facade.listMessages(tenantIdOrDefault(tenantId), sessionId, limit));
    }

    @PostMapping("/workspace/sessions/{sessionId}/work-item")
    public Mono<CompatibilityEnvelope<ConversationWorkItemView>> ensureWorkItem(
            @PathVariable Long sessionId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.ensureWorkItemForSession(
                tenantIdOrDefault(tenantId),
                sessionId,
                actorOrDefault(actor)));
    }

    @PostMapping("/workspace/work-items/{workItemId}/assign")
    public Mono<CompatibilityEnvelope<ConversationWorkItemView>> assign(
            @PathVariable Long workItemId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody ConversationAssignmentCommand command) {
        return envelope(() -> facade.assignWorkItem(
                tenantIdOrDefault(tenantId),
                workItemId,
                command,
                actorOrDefault(actor)));
    }

    @PostMapping("/workspace/work-items/{workItemId}/status")
    public Mono<CompatibilityEnvelope<ConversationWorkItemView>> updateStatus(
            @PathVariable Long workItemId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody ConversationWorkItemStatusCommand command) {
        return envelope(() -> facade.updateWorkItemStatus(
                tenantIdOrDefault(tenantId),
                workItemId,
                command,
                actorOrDefault(actor)));
    }

    @PostMapping("/workspace/routing-agents")
    public Mono<CompatibilityEnvelope<ConversationRoutingAgentView>> upsertRoutingAgent(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody ConversationRoutingAgentCommand command) {
        return envelope(() -> facade.upsertRoutingAgent(
                tenantIdOrDefault(tenantId),
                command,
                actorOrDefault(actor)));
    }

    @PostMapping("/workspace/routing-rules")
    public Mono<CompatibilityEnvelope<ConversationRoutingRuleView>> upsertRoutingRule(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody ConversationRoutingRuleCommand command) {
        return envelope(() -> facade.upsertRoutingRule(
                tenantIdOrDefault(tenantId),
                command,
                actorOrDefault(actor)));
    }

    @PostMapping("/workspace/work-items/{workItemId}/route")
    public Mono<CompatibilityEnvelope<ConversationRouteResultView>> routeWorkItem(
            @PathVariable Long workItemId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody ConversationRouteCommand command) {
        return envelope(() -> facade.routeWorkItem(
                tenantIdOrDefault(tenantId),
                workItemId,
                command,
                actorOrDefault(actor)));
    }

    @GetMapping("/workspace/inbox")
    public Mono<CompatibilityEnvelope<Object>> inbox(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(required = false) String channel,
            @RequestParam(defaultValue = "50") int limit) {
        return envelope(() -> facade.inbox(tenantIdOrDefault(tenantId), status, assignedTo, channel, limit));
    }

    @PostMapping("/workspace/work-items/{workItemId}/tasks")
    public Mono<CompatibilityEnvelope<Object>> createTask(
            @PathVariable Long workItemId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> command) {
        return envelope(() -> facade.createTask(tenantIdOrDefault(tenantId), workItemId, command, actorOrDefault(actor)));
    }

    @PostMapping("/workspace/tasks/{taskId}/complete")
    public Mono<CompatibilityEnvelope<Object>> completeTask(
            @PathVariable Long taskId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> command) {
        return envelope(() -> facade.completeTask(tenantIdOrDefault(tenantId), taskId, command, actorOrDefault(actor)));
    }

    @GetMapping("/workspace/work-items/{workItemId}/timeline")
    public Mono<CompatibilityEnvelope<Object>> timeline(
            @PathVariable Long workItemId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "50") int messageLimit,
            @RequestParam(defaultValue = "50") int auditLimit) {
        return envelope(() -> facade.timeline(tenantIdOrDefault(tenantId), workItemId, messageLimit, auditLimit));
    }

    @PostMapping("/workspace/sla-breaches/evaluate")
    public Mono<CompatibilityEnvelope<Object>> evaluateSlaBreaches(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestParam(defaultValue = "50") int limit) {
        return envelope(() -> facade.evaluateSlaBreaches(tenantIdOrDefault(tenantId), limit, actorOrDefault(actor)));
    }

    @GetMapping("/workspace/sla-breaches")
    public Mono<CompatibilityEnvelope<Object>> slaBreaches(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return envelope(() -> facade.slaBreaches(tenantIdOrDefault(tenantId), status, limit));
    }

    @PostMapping("/workspace/work-items/{workItemId}/ai-reply-suggestions/generate")
    public Mono<CompatibilityEnvelope<Object>> generateAiReplySuggestion(
            @PathVariable Long workItemId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> command) {
        return envelope(() -> facade.generateAiReplySuggestion(
                tenantIdOrDefault(tenantId),
                workItemId,
                command,
                actorOrDefault(actor)));
    }

    @PostMapping("/workspace/work-items/{workItemId}/ai-reply-suggestions/{suggestionId}/review")
    public Mono<CompatibilityEnvelope<Object>> reviewAiReplySuggestion(
            @PathVariable Long workItemId,
            @PathVariable Long suggestionId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> command) {
        return envelope(() -> facade.reviewAiReplySuggestion(
                tenantIdOrDefault(tenantId),
                workItemId,
                suggestionId,
                command,
                actorOrDefault(actor)));
    }

    @GetMapping("/workspace/work-items/{workItemId}/ai-reply-suggestions")
    public Mono<CompatibilityEnvelope<Object>> listAiReplySuggestions(
            @PathVariable Long workItemId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return envelope(() -> facade.listAiReplySuggestions(tenantIdOrDefault(tenantId), workItemId, status, limit));
    }

    @PostMapping("/private-domain/sync-runs")
    public Mono<CompatibilityEnvelope<Object>> ingestPrivateDomainSync(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> command) {
        return envelope(() -> facade.ingestPrivateDomainSync(
                tenantIdOrDefault(tenantId),
                command,
                actorOrDefault(actor)));
    }

    @GetMapping("/private-domain/contacts")
    public Mono<CompatibilityEnvelope<Object>> privateDomainContacts(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam String provider,
            @RequestParam(required = false) String ownerUserId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "50") int limit) {
        return envelope(() -> facade.privateDomainContacts(
                tenantIdOrDefault(tenantId),
                provider,
                ownerUserId,
                keyword,
                limit));
    }

    @GetMapping("/private-domain/groups")
    public Mono<CompatibilityEnvelope<Object>> privateDomainGroups(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam String provider,
            @RequestParam(required = false) String ownerUserId,
            @RequestParam(defaultValue = "50") int limit) {
        return envelope(() -> facade.privateDomainGroups(tenantIdOrDefault(tenantId), provider, ownerUserId, limit));
    }

    @GetMapping("/private-domain/sync-runs")
    public Mono<CompatibilityEnvelope<Object>> privateDomainSyncRuns(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam String provider,
            @RequestParam(defaultValue = "50") int limit) {
        return envelope(() -> facade.privateDomainSyncRuns(tenantIdOrDefault(tenantId), provider, limit));
    }

    @PostMapping("/provider-webhooks/whatsapp")
    public Mono<CompatibilityEnvelope<Object>> whatsAppWebhook(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.recordAdapterInbound(
                tenantIdOrDefault(tenantId),
                "WHATSAPP",
                payload,
                actorOrDefault(actor)));
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(ThrowingSupplier<T> supplier) {
        return Mono.fromCallable(() -> {
            try {
                return CompatibilityEnvelope.ok(supplier.get());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
            }
        });
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<CompatibilityEnvelope<Void>> handleResponseStatus(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        String message = exception.getReason() == null ? exception.getMessage() : exception.getReason();
        return ResponseEntity
                .status(exception.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(CompatibilityEnvelope.fail("API_001", status, message));
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    private record ConversationIngressRequest(
            Long canvasId,
            Long versionId,
            String executionId,
            String userId,
            String channel,
            String provider,
            String externalMessageId,
            String eventId,
            String messageType,
            String text,
            String intent,
            Map<String, Object> attributes,
            LocalDateTime occurredAt) {

        private ConversationInboundCommand toCommand(Long tenantId) {
            return new ConversationInboundCommand(
                    tenantId,
                    canvasId,
                    versionId,
                    executionId,
                    userId,
                    channel,
                    provider,
                    externalMessageId,
                    eventId,
                    messageType,
                    text,
                    intent,
                    attributes,
                    occurredAt);
        }
    }

    public record CompatibilityEnvelope<T>(
            int code,
            String message,
            String errorCode,
            T data,
            String traceId) {

        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static <T> CompatibilityEnvelope<T> fail(String errorCode, int code, String message) {
            return new CompatibilityEnvelope<>(code, message, errorCode, null, null);
        }
    }

    private interface ThrowingSupplier<T> {
        T get();
    }
}
