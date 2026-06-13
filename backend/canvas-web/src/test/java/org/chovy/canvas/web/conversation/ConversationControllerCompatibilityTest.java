package org.chovy.canvas.web.conversation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class ConversationControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final Long HEADER_TENANT_ID = 42L;
    private static final String DEFAULT_ACTOR = "operator-1";
    private static final String HEADER_ACTOR = "operator-2";
    private static final LocalDateTime NOW = LocalDateTime.parse("2026-06-12T14:20:53");

    @Test
    void conversationRoutesPreserveEnvelopeTenantActorHeadersAndCommandMapping() {
        RecordingConversationFacade facade = new RecordingConversationFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/canvas/conversations/ingress")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(inboundRequestJson())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.sessionId").isEqualTo(100)
                .jsonPath("$.data.messageId").isEqualTo(200)
                .jsonPath("$.data.status").isEqualTo("RECORDED")
                .jsonPath("$.data.duplicate").isEqualTo(false)
                .jsonPath("$.data.resumedWaitCount").isEqualTo(3);

        client.post()
                .uri("/canvas/conversations/workspace/sessions/{sessionId}/work-item", 555L)
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.id").isEqualTo(1)
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.sessionId").isEqualTo(555)
                .jsonPath("$.data.status").isEqualTo("OPEN")
                .jsonPath("$.data.routingStatus").isEqualTo("UNROUTED");

        client.post()
                .uri("/canvas/conversations/workspace/work-items/{workItemId}/assign", 88L)
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "assignedTo": "alice",
                          "assignedTeam": "sales",
                          "note": "VIP handoff"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.id").isEqualTo(88)
                .jsonPath("$.data.assignedTo").isEqualTo("alice")
                .jsonPath("$.data.assignedTeam").isEqualTo("sales");

        client.post()
                .uri("/canvas/conversations/workspace/work-items/{workItemId}/status", 88L)
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "status": "pending",
                          "priority": "high",
                          "nextFollowUpAt": "2026-06-12T16:30:00",
                          "note": "needs reply"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.id").isEqualTo(88)
                .jsonPath("$.data.status").isEqualTo("pending")
                .jsonPath("$.data.priority").isEqualTo("high")
                .jsonPath("$.data.nextFollowUpAt").isNotEmpty();

        client.post()
                .uri("/canvas/conversations/workspace/routing-agents")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "agentKey": "alice",
                          "displayName": "Alice",
                          "teamKey": "sales",
                          "status": "available",
                          "maxCapacity": 3,
                          "currentLoad": 1,
                          "skills": ["sales", "vip"],
                          "metadata": {"region": "east"}
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.agentKey").isEqualTo("alice")
                .jsonPath("$.data.skills[1]").isEqualTo("vip");

        client.post()
                .uri("/canvas/conversations/workspace/routing-rules")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "ruleKey": "vip-sales",
                          "channel": "whatsapp",
                          "minPriority": "high",
                          "requiredSkills": ["sales", "vip"],
                          "targetTeam": "sales",
                          "slaMinutes": 30,
                          "enabled": true,
                          "sortOrder": 10,
                          "metadata": {"segment": "vip"}
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.ruleKey").isEqualTo("vip-sales")
                .jsonPath("$.data.slaMinutes").isEqualTo(30);

        client.post()
                .uri("/canvas/conversations/workspace/work-items/{workItemId}/route", 88L)
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "requiredSkills": ["sales"],
                          "targetTeam": "sales",
                          "slaMinutes": 45,
                          "note": "route now"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.workItemId").isEqualTo(88)
                .jsonPath("$.data.routed").isEqualTo(true)
                .jsonPath("$.data.assignedTo").isEqualTo("alice")
                .jsonPath("$.data.requiredSkills[0]").isEqualTo("sales");

        assertThat(facade.inboundCommand).isEqualTo(new ConversationInboundCommand(
                HEADER_TENANT_ID,
                10L,
                20L,
                "exec-1",
                "user-1",
                " whatsapp ",
                "twilio",
                "msg-1",
                "evt-1",
                "text",
                "yes please",
                "PRODUCT_A",
                Map.of("locale", "en-US"),
                LocalDateTime.parse("2026-06-12T14:20:53")));
        assertThat(facade.tenantIds).containsExactly(
                HEADER_TENANT_ID,
                HEADER_TENANT_ID,
                HEADER_TENANT_ID,
                HEADER_TENANT_ID,
                HEADER_TENANT_ID,
                HEADER_TENANT_ID);
        assertThat(facade.actors).containsExactly(
                HEADER_ACTOR,
                HEADER_ACTOR,
                HEADER_ACTOR,
                HEADER_ACTOR,
                HEADER_ACTOR,
                HEADER_ACTOR);
        assertThat(facade.assignmentCommand).isEqualTo(new ConversationAssignmentCommand("alice", "sales", "VIP handoff"));
        assertThat(facade.statusCommand).isEqualTo(new ConversationWorkItemStatusCommand(
                "pending",
                "high",
                LocalDateTime.parse("2026-06-12T16:30:00"),
                "needs reply"));
        assertThat(facade.routingAgentCommand.agentKey()).isEqualTo("alice");
        assertThat(facade.routingRuleCommand.ruleKey()).isEqualTo("vip-sales");
        assertThat(facade.routeCommand).isEqualTo(new ConversationRouteCommand(
                List.of("sales"),
                "sales",
                45,
                "route now"));
    }

    @Test
    void conversationRoutesUseCompatibilityDefaultsAndTrimBlankActorHeaders() {
        RecordingConversationFacade facade = new RecordingConversationFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/canvas/conversations/ingress")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(inboundRequestJson())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.sessionId").isEqualTo(100);

        client.post()
                .uri("/canvas/conversations/workspace/sessions/{sessionId}/work-item", 777L)
                .header("X-Actor", "   ")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.sessionId").isEqualTo(777);

        assertThat(facade.inboundCommand.tenantId()).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.tenantIds).containsExactly(DEFAULT_TENANT_ID);
        assertThat(facade.actors).containsExactly(DEFAULT_ACTOR);
    }

    private static WebTestClient webClient(ConversationFacade facade) {
        return WebTestClient.bindToController(new ConversationController(facade)).build();
    }

    private static String inboundRequestJson() {
        return """
                {
                  "canvasId": 10,
                  "versionId": 20,
                  "executionId": "exec-1",
                  "userId": "user-1",
                  "channel": " whatsapp ",
                  "provider": "twilio",
                  "externalMessageId": "msg-1",
                  "eventId": "evt-1",
                  "messageType": "text",
                  "text": "yes please",
                  "intent": "PRODUCT_A",
                  "attributes": {"locale": "en-US"},
                  "occurredAt": "2026-06-12T14:20:53"
                }
                """;
    }

    private static final class RecordingConversationFacade implements ConversationFacade {
        private final List<Long> tenantIds = new ArrayList<>();
        private final List<String> actors = new ArrayList<>();
        private ConversationInboundCommand inboundCommand;
        private ConversationAssignmentCommand assignmentCommand;
        private ConversationWorkItemStatusCommand statusCommand;
        private ConversationRoutingAgentCommand routingAgentCommand;
        private ConversationRoutingRuleCommand routingRuleCommand;
        private ConversationRouteCommand routeCommand;

        @Override
        public ConversationRecordResult recordInbound(ConversationInboundCommand command) {
            inboundCommand = command;
            return new ConversationRecordResult(100L, 200L, "RECORDED", false, 3);
        }

        @Override
        public ConversationWorkItemView ensureWorkItemForSession(Long tenantId, Long sessionId, String actor) {
            recordMutation(tenantId, actor);
            return workItem(1L, tenantId, sessionId, "OPEN", "NORMAL", null, null, null);
        }

        @Override
        public ConversationWorkItemView assignWorkItem(
                Long tenantId,
                Long workItemId,
                ConversationAssignmentCommand command,
                String actor) {
            recordMutation(tenantId, actor);
            assignmentCommand = command;
            return workItem(workItemId, tenantId, 555L, "OPEN", "NORMAL", command.assignedTo(), command.assignedTeam(), null);
        }

        @Override
        public ConversationWorkItemView updateWorkItemStatus(
                Long tenantId,
                Long workItemId,
                ConversationWorkItemStatusCommand command,
                String actor) {
            recordMutation(tenantId, actor);
            statusCommand = command;
            return workItem(
                    workItemId,
                    tenantId,
                    555L,
                    command.status(),
                    command.priority(),
                    null,
                    null,
                    command.nextFollowUpAt());
        }

        @Override
        public ConversationRoutingAgentView upsertRoutingAgent(
                Long tenantId,
                ConversationRoutingAgentCommand command,
                String actor) {
            recordMutation(tenantId, actor);
            routingAgentCommand = command;
            return new ConversationRoutingAgentView(
                    11L,
                    tenantId,
                    command.agentKey(),
                    command.displayName(),
                    command.teamKey(),
                    command.status(),
                    command.maxCapacity(),
                    command.currentLoad(),
                    command.skills(),
                    command.metadata());
        }

        @Override
        public ConversationRoutingRuleView upsertRoutingRule(
                Long tenantId,
                ConversationRoutingRuleCommand command,
                String actor) {
            recordMutation(tenantId, actor);
            routingRuleCommand = command;
            return new ConversationRoutingRuleView(
                    12L,
                    tenantId,
                    command.ruleKey(),
                    command.channel(),
                    command.minPriority(),
                    command.requiredSkills(),
                    command.targetTeam(),
                    command.slaMinutes(),
                    command.enabled(),
                    command.sortOrder(),
                    command.metadata());
        }

        @Override
        public ConversationRouteResultView routeWorkItem(
                Long tenantId,
                Long workItemId,
                ConversationRouteCommand command,
                String actor) {
            recordMutation(tenantId, actor);
            routeCommand = command;
            return new ConversationRouteResultView(
                    tenantId,
                    workItemId,
                    true,
                    "ROUTED",
                    "alice",
                    command.targetTeam(),
                    command.requiredSkills(),
                    "matched rule",
                    NOW,
                    NOW.plusMinutes(command.slaMinutes()),
                    "rule");
        }

        private void recordMutation(Long tenantId, String actor) {
            tenantIds.add(tenantId);
            actors.add(actor);
        }

        private ConversationWorkItemView workItem(
                Long id,
                Long tenantId,
                Long sessionId,
                String status,
                String priority,
                String assignedTo,
                String assignedTeam,
                LocalDateTime nextFollowUpAt) {
            return new ConversationWorkItemView(
                    id,
                    tenantId,
                    sessionId,
                    "user-1",
                    "WHATSAPP",
                    "TWILIO",
                    "yes please",
                    status,
                    priority,
                    assignedTo,
                    assignedTeam,
                    "INBOUND",
                    NOW.plusHours(1),
                    nextFollowUpAt,
                    NOW,
                    NOW,
                    List.of("vip"),
                    Map.of("locale", "en-US"),
                    "UNROUTED",
                    List.of(),
                    null,
                    null,
                    null);
        }
    }
}
