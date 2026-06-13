package org.chovy.canvas.web.compat;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import org.chovy.canvas.conversation.application.ConversationApplicationService;
import org.chovy.canvas.conversation.domain.ConversationContactProfile;
import org.chovy.canvas.conversation.domain.ConversationMessage;
import org.chovy.canvas.conversation.domain.ConversationRoutingAgent;
import org.chovy.canvas.conversation.domain.ConversationRoutingRule;
import org.chovy.canvas.conversation.domain.ConversationSession;
import org.chovy.canvas.conversation.domain.ConversationSlaBreach;
import org.chovy.canvas.conversation.domain.ConversationWorkItem;
import org.chovy.canvas.conversation.domain.ConversationWorkItemAudit;
import org.chovy.canvas.conversation.domain.port.ConversationContactProfileRepository;
import org.chovy.canvas.conversation.domain.port.ConversationMessageRepository;
import org.chovy.canvas.conversation.domain.port.ConversationRoutingAgentRepository;
import org.chovy.canvas.conversation.domain.port.ConversationRoutingRuleRepository;
import org.chovy.canvas.conversation.domain.port.ConversationSessionRepository;
import org.chovy.canvas.conversation.domain.port.ConversationSlaBreachRepository;
import org.chovy.canvas.conversation.domain.port.ConversationWaitResumePort;
import org.chovy.canvas.conversation.domain.port.ConversationWorkItemAuditRepository;
import org.chovy.canvas.conversation.domain.port.ConversationWorkItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

class ConversationApiCompatibilityTest {

    private static final Long TENANT_ID = 7L;
    private static final String ACTOR = "operator-1";
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-05T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 5, 10, 0);

    @Test
    void ingressRoutePreservesSuccessEnvelopeAndDuplicateIdempotency() {
        Harness harness = Harness.create();
        WebTestClient client = webClient(harness.service());

        client.post()
                .uri("/canvas/conversations/ingress")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(inboundRequestJson("msg-1", "evt-1"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.sessionId").isEqualTo(1)
                .jsonPath("$.data.messageId").isEqualTo(1)
                .jsonPath("$.data.status").isEqualTo("RECORDED")
                .jsonPath("$.data.duplicate").isEqualTo(false)
                .jsonPath("$.data.resumedWaitCount").isEqualTo(1);

        client.post()
                .uri("/canvas/conversations/ingress")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(inboundRequestJson("msg-1", "evt-1"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.sessionId").isEqualTo(1)
                .jsonPath("$.data.messageId").isEqualTo(1)
                .jsonPath("$.data.status").isEqualTo("RECORDED")
                .jsonPath("$.data.duplicate").isEqualTo(true)
                .jsonPath("$.data.resumedWaitCount").isEqualTo(0);

        assertThat(harness.waits().calls).hasSize(1);
    }

    @Test
    void workItemCreationRoutePreservesWorkspaceEnvelope() {
        Harness harness = Harness.create();
        WebTestClient client = webClient(harness.service());
        ConversationRecordResult inbound = harness.service().recordInbound(inboundCommand("msg-work-item", "evt-work-item"));

        client.post()
                .uri("/canvas/conversations/workspace/sessions/{sessionId}/work-item", inbound.sessionId())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.id").isEqualTo(1)
                .jsonPath("$.data.sessionId").isEqualTo(inbound.sessionId().intValue())
                .jsonPath("$.data.userId").isEqualTo("user-1")
                .jsonPath("$.data.channel").isEqualTo("WHATSAPP")
                .jsonPath("$.data.provider").isEqualTo("TWILIO")
                .jsonPath("$.data.status").isEqualTo("OPEN")
                .jsonPath("$.data.priority").isEqualTo("NORMAL")
                .jsonPath("$.data.routingStatus").isEqualTo("UNROUTED");
    }

    @Test
    void assignmentAndStatusRoutesPreserveMutationEnvelopes() {
        Harness harness = Harness.create();
        ConversationWorkItemView workItem = seedWorkItem(harness, "msg-assignment");

        webClient(harness.service())
                .post()
                .uri("/canvas/conversations/workspace/work-items/{workItemId}/assign", workItem.id())
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
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.id").isEqualTo(workItem.id().intValue())
                .jsonPath("$.data.assignedTo").isEqualTo("alice")
                .jsonPath("$.data.assignedTeam").isEqualTo("sales")
                .jsonPath("$.data.status").isEqualTo("OPEN");

        webClient(harness.service())
                .post()
                .uri("/canvas/conversations/workspace/work-items/{workItemId}/status", workItem.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "status": "pending",
                          "priority": "high",
                          "nextFollowUpAt": "2026-06-05T12:00:00",
                          "note": "needs reply"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.id").isEqualTo(workItem.id().intValue())
                .jsonPath("$.data.status").isEqualTo("PENDING")
                .jsonPath("$.data.priority").isEqualTo("HIGH")
                .jsonPath("$.data.nextFollowUpAt").isNotEmpty();

        assertThat(harness.audits().saved).extracting(ConversationWorkItemAudit::eventType)
                .containsExactly("CREATED", "ASSIGNED", "STATUS_CHANGED");
    }

    @Test
    void routingAgentRuleAndRouteWorkItemRoutesPreserveRoutingEnvelope() {
        Harness harness = Harness.create();
        ConversationWorkItemView workItem = seedWorkItem(harness, "msg-routing");
        harness.service().updateWorkItemStatus(TENANT_ID, workItem.id(),
                new ConversationWorkItemStatusCommand("OPEN", "HIGH", null, "eligible for VIP routing"), ACTOR);
        WebTestClient client = webClient(harness.service());

        client.post()
                .uri("/canvas/conversations/workspace/routing-agents")
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
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.id").isEqualTo(1)
                .jsonPath("$.data.agentKey").isEqualTo("alice")
                .jsonPath("$.data.teamKey").isEqualTo("sales")
                .jsonPath("$.data.status").isEqualTo("AVAILABLE")
                .jsonPath("$.data.currentLoad").isEqualTo(1)
                .jsonPath("$.data.skills[0]").isEqualTo("sales")
                .jsonPath("$.data.skills[1]").isEqualTo("vip");

        client.post()
                .uri("/canvas/conversations/workspace/routing-rules")
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
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.id").isEqualTo(1)
                .jsonPath("$.data.ruleKey").isEqualTo("vip-sales")
                .jsonPath("$.data.channel").isEqualTo("WHATSAPP")
                .jsonPath("$.data.minPriority").isEqualTo("HIGH")
                .jsonPath("$.data.targetTeam").isEqualTo("sales")
                .jsonPath("$.data.slaMinutes").isEqualTo(30)
                .jsonPath("$.data.enabled").isEqualTo(true);

        client.post()
                .uri("/canvas/conversations/workspace/work-items/{workItemId}/route", workItem.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "requiredSkills": [],
                          "note": "route now"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.tenantId").isEqualTo(TENANT_ID.intValue())
                .jsonPath("$.data.workItemId").isEqualTo(workItem.id().intValue())
                .jsonPath("$.data.routed").isEqualTo(true)
                .jsonPath("$.data.routingStatus").isEqualTo("ROUTED")
                .jsonPath("$.data.assignedTo").isEqualTo("alice")
                .jsonPath("$.data.assignedTeam").isEqualTo("sales")
                .jsonPath("$.data.requiredSkills[0]").isEqualTo("sales")
                .jsonPath("$.data.requiredSkills[1]").isEqualTo("vip")
                .jsonPath("$.data.routingReason").isEqualTo("matched rule vip-sales to agent alice")
                .jsonPath("$.data.slaDueAt").isNotEmpty();

        assertThat(harness.agents().byKey(TENANT_ID, "alice").orElseThrow().currentLoad()).isEqualTo(2);
        assertThat(harness.audits().saved).extracting(ConversationWorkItemAudit::eventType)
                .contains("ROUTED");
    }

    private static WebTestClient webClient(ConversationFacade facade) {
        return WebTestClient.bindToController(new ConversationControllerAdapter(facade)).build();
    }

    private static ConversationWorkItemView seedWorkItem(Harness harness, String messageId) {
        ConversationRecordResult inbound = harness.service().recordInbound(inboundCommand(messageId, "evt-" + messageId));
        return harness.service().ensureWorkItemForSession(TENANT_ID, inbound.sessionId(), ACTOR);
    }

    private static ConversationInboundCommand inboundCommand(String messageId, String eventId) {
        return new ConversationInboundCommand(
                TENANT_ID,
                10L,
                20L,
                "exec-1",
                "user-1",
                " whatsapp ",
                "twilio",
                messageId,
                eventId,
                "text",
                "yes please",
                "PRODUCT_A",
                Map.of("locale", "en-US"),
                NOW);
    }

    private static String inboundRequestJson(String messageId, String eventId) {
        return """
                {
                  "canvasId": 10,
                  "versionId": 20,
                  "executionId": "exec-1",
                  "userId": "user-1",
                  "channel": " whatsapp ",
                  "provider": "twilio",
                  "externalMessageId": "%s",
                  "eventId": "%s",
                  "messageType": "text",
                  "text": "yes please",
                  "intent": "PRODUCT_A",
                  "attributes": {"locale": "en-US"},
                  "occurredAt": "2026-06-05T10:00:00"
                }
                """.formatted(messageId, eventId);
    }

    @RestController
    private static final class ConversationControllerAdapter {
        private final ConversationFacade facade;

        private ConversationControllerAdapter(ConversationFacade facade) {
            this.facade = facade;
        }

        @PostMapping("/canvas/conversations/ingress")
        Mono<CompatibilityEnvelope<ConversationRecordResult>> ingress(@RequestBody ConversationIngressRequest request) {
            return Mono.just(CompatibilityEnvelope.ok(facade.recordInbound(request.toCommand(TENANT_ID))));
        }

        @PostMapping("/canvas/conversations/workspace/sessions/{sessionId}/work-item")
        Mono<CompatibilityEnvelope<ConversationWorkItemView>> ensureWorkItem(@PathVariable Long sessionId) {
            return Mono.just(CompatibilityEnvelope.ok(facade.ensureWorkItemForSession(TENANT_ID, sessionId, ACTOR)));
        }

        @PostMapping("/canvas/conversations/workspace/work-items/{workItemId}/assign")
        Mono<CompatibilityEnvelope<ConversationWorkItemView>> assign(
                @PathVariable Long workItemId,
                @RequestBody ConversationAssignmentCommand command) {
            return Mono.just(CompatibilityEnvelope.ok(facade.assignWorkItem(TENANT_ID, workItemId, command, ACTOR)));
        }

        @PostMapping("/canvas/conversations/workspace/work-items/{workItemId}/status")
        Mono<CompatibilityEnvelope<ConversationWorkItemView>> updateStatus(
                @PathVariable Long workItemId,
                @RequestBody ConversationWorkItemStatusCommand command) {
            return Mono.just(CompatibilityEnvelope.ok(facade.updateWorkItemStatus(TENANT_ID, workItemId, command, ACTOR)));
        }

        @PostMapping("/canvas/conversations/workspace/routing-agents")
        Mono<CompatibilityEnvelope<ConversationRoutingAgentView>> upsertRoutingAgent(
                @RequestBody ConversationRoutingAgentCommand command) {
            return Mono.just(CompatibilityEnvelope.ok(facade.upsertRoutingAgent(TENANT_ID, command, ACTOR)));
        }

        @PostMapping("/canvas/conversations/workspace/routing-rules")
        Mono<CompatibilityEnvelope<ConversationRoutingRuleView>> upsertRoutingRule(
                @RequestBody ConversationRoutingRuleCommand command) {
            return Mono.just(CompatibilityEnvelope.ok(facade.upsertRoutingRule(TENANT_ID, command, ACTOR)));
        }

        @PostMapping("/canvas/conversations/workspace/work-items/{workItemId}/route")
        Mono<CompatibilityEnvelope<ConversationRouteResultView>> routeWorkItem(
                @PathVariable Long workItemId,
                @RequestBody ConversationRouteCommand command) {
            return Mono.just(CompatibilityEnvelope.ok(facade.routeWorkItem(TENANT_ID, workItemId, command, ACTOR)));
        }
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

    private record CompatibilityEnvelope<T>(
            int code,
            String message,
            String errorCode,
            T data,
            String traceId) {

        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }
    }

    private record Harness(ConversationApplicationService service,
                           InMemorySessions sessions,
                           InMemoryMessages messages,
                           InMemoryProfiles profiles,
                           InMemoryWorkItems workItems,
                           InMemoryAudits audits,
                           InMemoryAgents agents,
                           InMemoryRules rules,
                           RecordingWaitResumePort waits) {

        static Harness create() {
            InMemorySessions sessions = new InMemorySessions();
            InMemoryMessages messages = new InMemoryMessages();
            InMemoryProfiles profiles = new InMemoryProfiles();
            InMemoryWorkItems workItems = new InMemoryWorkItems();
            InMemoryAudits audits = new InMemoryAudits();
            InMemoryAgents agents = new InMemoryAgents();
            InMemoryRules rules = new InMemoryRules();
            InMemoryBreaches breaches = new InMemoryBreaches();
            RecordingWaitResumePort waits = new RecordingWaitResumePort();
            return new Harness(new ConversationApplicationService(
                    sessions, messages, profiles, workItems, audits, agents, rules, breaches, waits, CLOCK),
                    sessions, messages, profiles, workItems, audits, agents, rules, waits);
        }
    }

    private static final class InMemorySessions implements ConversationSessionRepository {
        private final List<ConversationSession> saved = new ArrayList<>();
        private long sequence = 1L;

        @Override
        public Optional<ConversationSession> findActive(Long tenantId, String userId, String channel, String provider, String executionId) {
            return saved.stream()
                    .filter(session -> tenantId.equals(session.tenantId()))
                    .filter(session -> userId.equals(session.userId()))
                    .filter(session -> channel.equals(session.channel()))
                    .filter(session -> provider.equals(session.provider()))
                    .filter(session -> executionId == null ? session.executionId() == null : executionId.equals(session.executionId()))
                    .filter(session -> "ACTIVE".equals(session.status()))
                    .findFirst();
        }

        @Override
        public Optional<ConversationSession> byId(Long id) {
            return saved.stream().filter(session -> id.equals(session.id())).findFirst();
        }

        @Override
        public ConversationSession save(ConversationSession session) {
            ConversationSession savedSession = session.id() == null ? session.withId(sequence++) : session;
            saved.removeIf(existing -> savedSession.id().equals(existing.id()));
            saved.add(savedSession);
            return savedSession;
        }
    }

    private static final class InMemoryMessages implements ConversationMessageRepository {
        private final List<ConversationMessage> saved = new ArrayList<>();
        private long sequence = 1L;

        @Override
        public Optional<ConversationMessage> byIdempotencyKey(Long tenantId, String idempotencyKey) {
            return saved.stream()
                    .filter(message -> tenantId.equals(message.tenantId()))
                    .filter(message -> idempotencyKey.equals(message.idempotencyKey()))
                    .findFirst();
        }

        @Override
        public ConversationMessage save(ConversationMessage message) {
            ConversationMessage savedMessage = message.id() == null ? message.withId(sequence++) : message;
            saved.removeIf(existing -> savedMessage.id().equals(existing.id()));
            saved.add(savedMessage);
            return savedMessage;
        }
    }

    private static final class InMemoryProfiles implements ConversationContactProfileRepository {
        private final List<ConversationContactProfile> saved = new ArrayList<>();
        private long sequence = 1L;

        @Override
        public Optional<ConversationContactProfile> byUser(Long tenantId, String userId) {
            return saved.stream()
                    .filter(profile -> tenantId.equals(profile.tenantId()))
                    .filter(profile -> userId.equals(profile.userId()))
                    .findFirst();
        }

        @Override
        public ConversationContactProfile save(ConversationContactProfile profile) {
            ConversationContactProfile savedProfile = profile.id() == null ? profile.withId(sequence++) : profile;
            saved.removeIf(existing -> savedProfile.id().equals(existing.id()));
            saved.add(savedProfile);
            return savedProfile;
        }
    }

    private static final class InMemoryWorkItems implements ConversationWorkItemRepository {
        private final List<ConversationWorkItem> saved = new ArrayList<>();
        private long sequence = 1L;

        @Override
        public Optional<ConversationWorkItem> bySession(Long tenantId, Long sessionId) {
            return saved.stream()
                    .filter(item -> tenantId.equals(item.tenantId()))
                    .filter(item -> sessionId.equals(item.sessionId()))
                    .findFirst();
        }

        @Override
        public Optional<ConversationWorkItem> byId(Long id) {
            return saved.stream().filter(item -> id.equals(item.id())).findFirst();
        }

        @Override
        public List<ConversationWorkItem> dueForSla(Long tenantId, LocalDateTime now, int limit) {
            return saved.stream()
                    .filter(item -> tenantId.equals(item.tenantId()))
                    .filter(item -> item.slaDueAt() != null && !item.slaDueAt().isAfter(now))
                    .limit(limit)
                    .toList();
        }

        @Override
        public ConversationWorkItem save(ConversationWorkItem item) {
            ConversationWorkItem savedItem = item.id() == null ? item.withId(sequence++) : item;
            saved.removeIf(existing -> savedItem.id().equals(existing.id()));
            saved.add(savedItem);
            return savedItem;
        }
    }

    private static final class InMemoryAudits implements ConversationWorkItemAuditRepository {
        private final List<ConversationWorkItemAudit> saved = new ArrayList<>();
        private long sequence = 1L;

        @Override
        public ConversationWorkItemAudit save(ConversationWorkItemAudit audit) {
            ConversationWorkItemAudit savedAudit = audit.id() == null ? audit.withId(sequence++) : audit;
            saved.add(savedAudit);
            return savedAudit;
        }
    }

    private static final class InMemoryAgents implements ConversationRoutingAgentRepository {
        private final List<ConversationRoutingAgent> saved = new ArrayList<>();

        @Override
        public Optional<ConversationRoutingAgent> byKey(Long tenantId, String agentKey) {
            return saved.stream()
                    .filter(agent -> tenantId.equals(agent.tenantId()))
                    .filter(agent -> agentKey.equals(agent.agentKey()))
                    .findFirst();
        }

        @Override
        public List<ConversationRoutingAgent> candidates(Long tenantId, String teamKey) {
            return saved.stream()
                    .filter(agent -> tenantId.equals(agent.tenantId()))
                    .filter(agent -> teamKey == null || teamKey.equals(agent.teamKey()))
                    .toList();
        }

        @Override
        public ConversationRoutingAgent save(ConversationRoutingAgent agent) {
            ConversationRoutingAgent savedAgent = agent.id() == null ? agent.withId((long) saved.size() + 1) : agent;
            saved.removeIf(existing -> savedAgent.id().equals(existing.id()));
            saved.add(savedAgent);
            return savedAgent;
        }
    }

    private static final class InMemoryRules implements ConversationRoutingRuleRepository {
        private final List<ConversationRoutingRule> saved = new ArrayList<>();

        @Override
        public Optional<ConversationRoutingRule> byKey(Long tenantId, String ruleKey) {
            return saved.stream()
                    .filter(rule -> tenantId.equals(rule.tenantId()))
                    .filter(rule -> ruleKey.equals(rule.ruleKey()))
                    .findFirst();
        }

        @Override
        public List<ConversationRoutingRule> enabled(Long tenantId) {
            return saved.stream()
                    .filter(rule -> tenantId.equals(rule.tenantId()))
                    .filter(ConversationRoutingRule::enabled)
                    .toList();
        }

        @Override
        public ConversationRoutingRule save(ConversationRoutingRule rule) {
            ConversationRoutingRule savedRule = rule.id() == null ? rule.withId((long) saved.size() + 1) : rule;
            saved.removeIf(existing -> savedRule.id().equals(existing.id()));
            saved.add(savedRule);
            return savedRule;
        }
    }

    private static final class InMemoryBreaches implements ConversationSlaBreachRepository {
        private final Map<Long, ConversationSlaBreach> saved = new LinkedHashMap<>();

        @Override
        public Optional<ConversationSlaBreach> openByWorkItem(Long tenantId, Long workItemId) {
            return Optional.ofNullable(saved.get(workItemId))
                    .filter(breach -> tenantId.equals(breach.tenantId()))
                    .filter(breach -> "OPEN".equals(breach.status()));
        }

        @Override
        public ConversationSlaBreach save(ConversationSlaBreach breach) {
            ConversationSlaBreach savedBreach = breach.id() == null ? breach.withId((long) saved.size() + 1) : breach;
            saved.put(savedBreach.workItemId(), savedBreach);
            return savedBreach;
        }
    }

    private static final class RecordingWaitResumePort implements ConversationWaitResumePort {
        private final List<Call> calls = new ArrayList<>();

        @Override
        public int resumeEventWaits(String eventCode, String subject, Map<String, Object> attributes, String eventId) {
            calls.add(new Call(eventCode, subject, attributes, eventId));
            return 1;
        }
    }

    private record Call(String eventCode, String subject, Map<String, Object> attributes, String eventId) {
    }
}
