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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
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
