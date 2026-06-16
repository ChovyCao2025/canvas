package org.chovy.canvas.conversation.application;

import org.chovy.canvas.conversation.api.ConversationAssignmentCommand;
import org.chovy.canvas.conversation.api.ConversationInboundCommand;
import org.chovy.canvas.conversation.api.ConversationRecordResult;
import org.chovy.canvas.conversation.api.ConversationRouteCommand;
import org.chovy.canvas.conversation.api.ConversationRoutingAgentCommand;
import org.chovy.canvas.conversation.api.ConversationRoutingRuleCommand;
import org.chovy.canvas.conversation.api.ConversationWorkItemStatusCommand;
import org.chovy.canvas.conversation.api.ConversationWorkItemView;
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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证会话应用服务写路径和路由路径的单元测试。
 */
class ConversationApplicationServiceTest {

    /**
     * 固定应用服务测试时钟。
     */
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-05T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    /**
     * 固定业务时间，便于断言 SLA 和更新时间。
     */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 5, 10, 0);

    /**
     * 验证入站回复会创建会话、消息并恢复等待节点。
     */
    @Test
    void inboundReplyCreatesSessionMessageAndResumesWaitsWithoutExecutionDependency() {
        Harness harness = Harness.create();

        ConversationRecordResult result = harness.service.recordInbound(new ConversationInboundCommand(
                7L,
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
                NOW));

        assertThat(result.sessionId()).isEqualTo(1L);
        assertThat(result.messageId()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo("RECORDED");
        assertThat(result.duplicate()).isFalse();
        assertThat(result.resumedWaitCount()).isEqualTo(1);

        ConversationSession session = harness.sessions.saved.get(0);
        assertThat(session.tenantId()).isEqualTo(7L);
        assertThat(session.channel()).isEqualTo("WHATSAPP");
        assertThat(session.provider()).isEqualTo("TWILIO");
        assertThat(session.status()).isEqualTo("ACTIVE");
        assertThat(session.turnCount()).isEqualTo(1);
        assertThat(session.context()).containsEntry("intent", "PRODUCT_A")
                .containsEntry("lastText", "yes please")
                .containsEntry("lastMessageId", 1L);

        ConversationMessage message = harness.messages.saved.get(0);
        assertThat(message.direction()).isEqualTo("INBOUND");
        assertThat(message.messageType()).isEqualTo("TEXT");
        assertThat(message.idempotencyKey()).isEqualTo("WHATSAPP:TWILIO:msg-1");
        assertThat(message.content()).containsEntry("text", "yes please")
                .containsEntry("intent", "PRODUCT_A");

        assertThat(harness.waits.calls).hasSize(1);
        assertThat(harness.waits.calls.get(0).eventCode).isEqualTo("CONVERSATION_REPLY");
        assertThat(harness.waits.calls.get(0).subject).isEqualTo("user-1");
        assertThat(harness.waits.calls.get(0).eventId).isEqualTo("evt-1");
        assertThat(harness.waits.calls.get(0).attributes)
                .containsEntry("conversationSessionId", 1L)
                .containsEntry("conversationMessageId", 1L)
                .containsEntry("channel", "WHATSAPP")
                .containsEntry("provider", "TWILIO");
    }

    /**
     * 验证重复入站消息只返回已有消息，不产生额外副作用。
     */
    @Test
    void duplicateInboundReplyReturnsExistingMessageWithoutSideEffects() {
        Harness harness = Harness.create();
        harness.messages.saved.add(new ConversationMessage(
                99L, 7L, 42L, "INBOUND", "TEXT", "msg-1",
                "WHATSAPP:TWILIO:msg-1", Map.of(), "yes", "PRODUCT_A", false, NOW));

        ConversationRecordResult result = harness.service.recordInbound(new ConversationInboundCommand(
                7L, 10L, 20L, "exec-1", "user-1", "whatsapp", "twilio",
                "msg-1", "evt-1", "text", "yes", "PRODUCT_A", Map.of(), NOW));

        assertThat(result.sessionId()).isEqualTo(42L);
        assertThat(result.messageId()).isEqualTo(99L);
        assertThat(result.duplicate()).isTrue();
        assertThat(result.resumedWaitCount()).isZero();
        assertThat(harness.sessions.saved).isEmpty();
        assertThat(harness.waits.calls).isEmpty();
    }

    /**
     * 验证从会话创建工单，并记录分配和状态变更审计。
     */
    @Test
    void createsWorkItemFromSessionAndAuditsStatusAndAssignmentChanges() {
        Harness harness = Harness.create();
        harness.sessions.saved.add(new ConversationSession(
                10L, 7L, 100L, 200L, "exec-1", "user-1", "web_chat", "widget",
                "ACTIVE", 2, Map.of("segment", "vip"), NOW.minusMinutes(3),
                null, NOW.minusMinutes(10), NOW.minusMinutes(3)));

        ConversationWorkItemView created = harness.service.ensureWorkItemForSession(7L, 10L, "agent-lead");
        ConversationWorkItemView assigned = harness.service.assignWorkItem(7L, created.id(),
                new ConversationAssignmentCommand("alice", "sales", "manual handoff"), "agent-lead");
        ConversationWorkItemView updated = harness.service.updateWorkItemStatus(7L, created.id(),
                new ConversationWorkItemStatusCommand("pending", "high", NOW.plusHours(2), "needs reply"),
                "alice");

        assertThat(created.status()).isEqualTo("OPEN");
        assertThat(created.routingStatus()).isEqualTo("UNROUTED");
        assertThat(assigned.assignedTo()).isEqualTo("alice");
        assertThat(updated.status()).isEqualTo("PENDING");
        assertThat(updated.priority()).isEqualTo("HIGH");
        assertThat(harness.profiles.saved).hasSize(1);
        assertThat(harness.audits.saved).extracting(ConversationWorkItemAudit::eventType)
                .containsExactly("CREATED", "ASSIGNED", "STATUS_CHANGED");
    }

    /**
     * 验证路由选择最低负载坐席，并在容量不足时记录路由失败。
     */
    @Test
    void routesWorkItemToLowestLoadAgentAndRecordsRouteMissWhenCapacityIsUnavailable() {
        Harness harness = Harness.create();
        harness.workItems.saved.add(new ConversationWorkItem(
                50L, 7L, 10L, 20L, "user-1", "WEB_CHAT", "WIDGET",
                "WEB_CHAT conversation with user-1", "OPEN", "HIGH", null, null,
                "CONVERSATION", null, null, NOW.minusMinutes(5), null,
                List.of(), Map.of(), "UNROUTED", List.of(), null, null, null,
                NOW.minusMinutes(10), NOW.minusMinutes(5)));
        harness.rules.saved.add(new ConversationRoutingRule(
                70L, 7L, "vip-sales", "WEB_CHAT", "HIGH",
                List.of("sales", "vip"), "sales", 30, true, 10, Map.of(),
                "manager", NOW.minusDays(1), NOW.minusDays(1)));
        harness.agents.saved.add(new ConversationRoutingAgent(
                80L, 7L, "bob", "Bob", "sales", "AVAILABLE", 3, 2,
                List.of("sales", "vip"), Map.of(), "manager", NOW.minusDays(1), NOW.minusDays(1)));
        harness.agents.saved.add(new ConversationRoutingAgent(
                81L, 7L, "alice", "Alice", "sales", "AVAILABLE", 3, 1,
                List.of("sales", "vip"), Map.of(), "manager", NOW.minusDays(1), NOW.minusDays(1)));

        var routed = harness.service.routeWorkItem(7L, 50L,
                new ConversationRouteCommand(List.of(), null, null, "route now"), "router");

        assertThat(routed.routed()).isTrue();
        assertThat(routed.assignedTo()).isEqualTo("alice");
        assertThat(routed.requiredSkills()).containsExactly("sales", "vip");
        assertThat(routed.slaDueAt()).isEqualTo(NOW.plusMinutes(30));
        assertThat(harness.workItems.byId(50L).orElseThrow().routingStatus()).isEqualTo("ROUTED");
        assertThat(harness.agents.byKey(7L, "alice").orElseThrow().currentLoad()).isEqualTo(2);

        harness.agents.saved.replaceAll(agent -> agent.withCurrentLoad(agent.maxCapacity()));
        harness.workItems.saved.set(0, harness.workItems.byId(50L).orElseThrow()
                .withRouting("UNROUTED", null, null, null, null, null, null));

        var missed = harness.service.routeWorkItem(7L, 50L,
                new ConversationRouteCommand(List.of("sales", "vip"), "sales", 30, "retry"),
                "router");

        assertThat(missed.routed()).isFalse();
        assertThat(missed.routingStatus()).isEqualTo("UNROUTED");
        assertThat(missed.routingReason()).contains("no available agent");
        assertThat(harness.audits.saved).extracting(ConversationWorkItemAudit::eventType)
                .contains("ROUTED", "ROUTING_MISSED");
    }

    /**
     * 应用服务测试夹具，集中持有服务和内存端口。
     *
     * @param service 待测应用服务
     * @param sessions 内存会话仓储
     * @param messages 内存消息仓储
     * @param profiles 内存联系人画像仓储
     * @param workItems 内存工单仓储
     * @param audits 内存审计仓储
     * @param agents 内存坐席仓储
     * @param rules 内存规则仓储
     * @param waits 等待恢复记录端口
     */
    private record Harness(
            /**
             * 待测应用服务。
             */
            ConversationApplicationService service,
            /**
             * 内存会话仓储。
             */
            InMemorySessions sessions,
            /**
             * 内存消息仓储。
             */
            InMemoryMessages messages,
            /**
             * 内存联系人画像仓储。
             */
            InMemoryProfiles profiles,
            /**
             * 内存工单仓储。
             */
            InMemoryWorkItems workItems,
            /**
             * 内存审计仓储。
             */
            InMemoryAudits audits,
            /**
             * 内存坐席仓储。
             */
            InMemoryAgents agents,
            /**
             * 内存规则仓储。
             */
            InMemoryRules rules,
            /**
             * 等待恢复记录端口。
             */
            RecordingWaitResumePort waits) {

        /**
         * 创建一组全新的内存依赖和待测服务。
         *
         * @return 测试夹具
         */
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

    /**
     * 会话仓储的内存实现。
     */
    private static final class InMemorySessions implements ConversationSessionRepository {
        /**
         * 已保存会话列表。
         */
        private final List<ConversationSession> saved = new ArrayList<>();

        /**
         * 模拟数据库自增序列。
         */
        private long sequence = 1L;

        /**
         * 查询匹配条件的活跃会话。
         *
         * @param tenantId 租户标识
         * @param userId 用户标识
         * @param channel 渠道
         * @param provider 服务商
         * @param executionId 执行标识
         * @return 匹配会话
         */
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

        /**
         * 按主键查询会话。
         *
         * @param id 会话标识
         * @return 匹配会话
         */
        @Override
        public Optional<ConversationSession> byId(Long id) {
            return saved.stream().filter(session -> id.equals(session.id())).findFirst();
        }

        /**
         * 保存会话并模拟生成主键。
         *
         * @param session 待保存会话
         * @return 保存后的会话
         */
        @Override
        public ConversationSession save(ConversationSession session) {
            ConversationSession savedSession = session.id() == null ? session.withId(sequence++) : session;
            saved.removeIf(existing -> savedSession.id().equals(existing.id()));
            saved.add(savedSession);
            return savedSession;
        }
    }

    /**
     * 会话消息仓储的内存实现。
     */
    private static final class InMemoryMessages implements ConversationMessageRepository {
        /**
         * 已保存消息列表。
         */
        private final List<ConversationMessage> saved = new ArrayList<>();

        /**
         * 模拟数据库自增序列。
         */
        private long sequence = 1L;

        /**
         * 按租户和幂等键查询消息。
         *
         * @param tenantId 租户标识
         * @param idempotencyKey 幂等键
         * @return 匹配消息
         */
        @Override
        public Optional<ConversationMessage> byIdempotencyKey(Long tenantId, String idempotencyKey) {
            return saved.stream()
                    .filter(message -> tenantId.equals(message.tenantId()))
                    .filter(message -> idempotencyKey.equals(message.idempotencyKey()))
                    .findFirst();
        }

        /**
         * 保存消息并模拟生成主键。
         *
         * @param message 待保存消息
         * @return 保存后的消息
         */
        @Override
        public ConversationMessage save(ConversationMessage message) {
            ConversationMessage savedMessage = message.id() == null ? message.withId(sequence++) : message;
            saved.removeIf(existing -> savedMessage.id().equals(existing.id()));
            saved.add(savedMessage);
            return savedMessage;
        }
    }

    /**
     * 联系人画像仓储的内存实现。
     */
    private static final class InMemoryProfiles implements ConversationContactProfileRepository {
        /**
         * 已保存联系人画像列表。
         */
        private final List<ConversationContactProfile> saved = new ArrayList<>();

        /**
         * 模拟数据库自增序列。
         */
        private long sequence = 1L;

        /**
         * 按租户和用户标识查询联系人画像。
         *
         * @param tenantId 租户标识
         * @param userId 用户标识
         * @return 匹配联系人画像
         */
        @Override
        public Optional<ConversationContactProfile> byUser(Long tenantId, String userId) {
            return saved.stream()
                    .filter(profile -> tenantId.equals(profile.tenantId()))
                    .filter(profile -> userId.equals(profile.userId()))
                    .findFirst();
        }

        /**
         * 保存联系人画像并模拟生成主键。
         *
         * @param profile 待保存联系人画像
         * @return 保存后的联系人画像
         */
        @Override
        public ConversationContactProfile save(ConversationContactProfile profile) {
            ConversationContactProfile savedProfile = profile.id() == null ? profile.withId(sequence++) : profile;
            saved.removeIf(existing -> savedProfile.id().equals(existing.id()));
            saved.add(savedProfile);
            return savedProfile;
        }
    }

    /**
     * 会话工单仓储的内存实现。
     */
    private static final class InMemoryWorkItems implements ConversationWorkItemRepository {
        /**
         * 已保存工单列表。
         */
        private final List<ConversationWorkItem> saved = new ArrayList<>();

        /**
         * 模拟数据库自增序列。
         */
        private long sequence = 1L;

        /**
         * 按租户和会话查询工单。
         *
         * @param tenantId 租户标识
         * @param sessionId 会话标识
         * @return 匹配工单
         */
        @Override
        public Optional<ConversationWorkItem> bySession(Long tenantId, Long sessionId) {
            return saved.stream()
                    .filter(item -> tenantId.equals(item.tenantId()))
                    .filter(item -> sessionId.equals(item.sessionId()))
                    .findFirst();
        }

        /**
         * 按主键查询工单。
         *
         * @param id 工单标识
         * @return 匹配工单
         */
        @Override
        public Optional<ConversationWorkItem> byId(Long id) {
            return saved.stream().filter(item -> id.equals(item.id())).findFirst();
        }

        /**
         * 查询已到 SLA 时间的工单。
         *
         * @param tenantId 租户标识
         * @param now SLA 检查基准时间
         * @param limit 返回数量上限
         * @return 到期工单列表
         */
        @Override
        public List<ConversationWorkItem> dueForSla(Long tenantId, LocalDateTime now, int limit) {
            return saved.stream()
                    .filter(item -> tenantId.equals(item.tenantId()))
                    .filter(item -> item.slaDueAt() != null && !item.slaDueAt().isAfter(now))
                    .limit(limit)
                    .toList();
        }

        /**
         * 保存工单并模拟生成主键。
         *
         * @param item 待保存工单
         * @return 保存后的工单
         */
        @Override
        public ConversationWorkItem save(ConversationWorkItem item) {
            ConversationWorkItem savedItem = item.id() == null ? item.withId(sequence++) : item;
            saved.removeIf(existing -> savedItem.id().equals(existing.id()));
            saved.add(savedItem);
            return savedItem;
        }
    }

    /**
     * 工单审计仓储的内存实现。
     */
    private static final class InMemoryAudits implements ConversationWorkItemAuditRepository {
        /**
         * 已保存审计事件列表。
         */
        private final List<ConversationWorkItemAudit> saved = new ArrayList<>();

        /**
         * 模拟数据库自增序列。
         */
        private long sequence = 1L;

        /**
         * 保存审计事件并模拟生成主键。
         *
         * @param audit 待保存审计事件
         * @return 保存后的审计事件
         */
        @Override
        public ConversationWorkItemAudit save(ConversationWorkItemAudit audit) {
            ConversationWorkItemAudit savedAudit = audit.id() == null ? audit.withId(sequence++) : audit;
            saved.add(savedAudit);
            return savedAudit;
        }
    }

    /**
     * 路由坐席仓储的内存实现。
     */
    private static final class InMemoryAgents implements ConversationRoutingAgentRepository {
        /**
         * 已保存坐席列表。
         */
        private final List<ConversationRoutingAgent> saved = new ArrayList<>();

        /**
         * 按租户和坐席键查询坐席。
         *
         * @param tenantId 租户标识
         * @param agentKey 坐席业务键
         * @return 匹配坐席
         */
        @Override
        public Optional<ConversationRoutingAgent> byKey(Long tenantId, String agentKey) {
            return saved.stream()
                    .filter(agent -> tenantId.equals(agent.tenantId()))
                    .filter(agent -> agentKey.equals(agent.agentKey()))
                    .findFirst();
        }

        /**
         * 查询指定团队的候选坐席。
         *
         * @param tenantId 租户标识
         * @param teamKey 团队键
         * @return 候选坐席列表
         */
        @Override
        public List<ConversationRoutingAgent> candidates(Long tenantId, String teamKey) {
            return saved.stream()
                    .filter(agent -> tenantId.equals(agent.tenantId()))
                    .filter(agent -> teamKey == null || teamKey.equals(agent.teamKey()))
                    .toList();
        }

        /**
         * 保存坐席并模拟生成主键。
         *
         * @param agent 待保存坐席
         * @return 保存后的坐席
         */
        @Override
        public ConversationRoutingAgent save(ConversationRoutingAgent agent) {
            ConversationRoutingAgent savedAgent = agent.id() == null ? agent.withId((long) saved.size() + 1) : agent;
            saved.removeIf(existing -> savedAgent.id().equals(existing.id()));
            saved.add(savedAgent);
            return savedAgent;
        }
    }

    /**
     * 路由规则仓储的内存实现。
     */
    private static final class InMemoryRules implements ConversationRoutingRuleRepository {
        /**
         * 已保存规则列表。
         */
        private final List<ConversationRoutingRule> saved = new ArrayList<>();

        /**
         * 按租户和规则键查询规则。
         *
         * @param tenantId 租户标识
         * @param ruleKey 规则业务键
         * @return 匹配规则
         */
        @Override
        public Optional<ConversationRoutingRule> byKey(Long tenantId, String ruleKey) {
            return saved.stream()
                    .filter(rule -> tenantId.equals(rule.tenantId()))
                    .filter(rule -> ruleKey.equals(rule.ruleKey()))
                    .findFirst();
        }

        /**
         * 查询租户内启用规则。
         *
         * @param tenantId 租户标识
         * @return 启用规则列表
         */
        @Override
        public List<ConversationRoutingRule> enabled(Long tenantId) {
            return saved.stream()
                    .filter(rule -> tenantId.equals(rule.tenantId()))
                    .filter(ConversationRoutingRule::enabled)
                    .toList();
        }

        /**
         * 保存规则并模拟生成主键。
         *
         * @param rule 待保存规则
         * @return 保存后的规则
         */
        @Override
        public ConversationRoutingRule save(ConversationRoutingRule rule) {
            ConversationRoutingRule savedRule = rule.id() == null ? rule.withId((long) saved.size() + 1) : rule;
            saved.removeIf(existing -> savedRule.id().equals(existing.id()));
            saved.add(savedRule);
            return savedRule;
        }
    }

    /**
     * SLA 违约仓储的内存实现。
     */
    private static final class InMemoryBreaches implements ConversationSlaBreachRepository {
        /**
         * 按工单保存的 SLA 违约记录。
         */
        private final Map<Long, ConversationSlaBreach> saved = new LinkedHashMap<>();

        /**
         * 查询工单当前打开的 SLA 违约记录。
         *
         * @param tenantId 租户标识
         * @param workItemId 工单标识
         * @return 打开的 SLA 违约记录
         */
        @Override
        public Optional<ConversationSlaBreach> openByWorkItem(Long tenantId, Long workItemId) {
            return Optional.ofNullable(saved.get(workItemId))
                    .filter(breach -> tenantId.equals(breach.tenantId()))
                    .filter(breach -> "OPEN".equals(breach.status()));
        }

        /**
         * 保存 SLA 违约记录并模拟生成主键。
         *
         * @param breach 待保存违约记录
         * @return 保存后的违约记录
         */
        @Override
        public ConversationSlaBreach save(ConversationSlaBreach breach) {
            ConversationSlaBreach savedBreach = breach.id() == null ? breach.withId((long) saved.size() + 1) : breach;
            saved.put(savedBreach.workItemId(), savedBreach);
            return savedBreach;
        }
    }

    /**
     * 记录等待恢复调用的测试端口。
     */
    private static final class RecordingWaitResumePort implements ConversationWaitResumePort {
        /**
         * 已记录的恢复调用。
         */
        private final List<Call> calls = new ArrayList<>();

        /**
         * 记录一次等待恢复调用并返回固定恢复数量。
         *
         * @param eventCode 事件编码
         * @param subject 事件主题
         * @param attributes 事件属性
         * @param eventId 事件标识
         * @return 固定恢复数量
         */
        @Override
        public int resumeEventWaits(String eventCode, String subject, Map<String, Object> attributes, String eventId) {
            calls.add(new Call(eventCode, subject, attributes, eventId));
            return 1;
        }
    }

    /**
     * 等待恢复调用记录。
     *
     * @param eventCode 事件编码
     * @param subject 事件主题
     * @param attributes 事件属性
     * @param eventId 事件标识
     */
    private record Call(
            /**
             * 事件编码。
             */
            String eventCode,
            /**
             * 事件主题。
             */
            String subject,
            /**
             * 事件属性。
             */
            Map<String, Object> attributes,
            /**
             * 事件标识。
             */
            String eventId) {
    }
}
