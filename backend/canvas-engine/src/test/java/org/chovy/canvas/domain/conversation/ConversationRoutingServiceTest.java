package org.chovy.canvas.domain.conversation;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.ConversationRoutingAgentDO;
import org.chovy.canvas.dal.dataobject.ConversationRoutingRuleDO;
import org.chovy.canvas.dal.dataobject.ConversationSlaBreachDO;
import org.chovy.canvas.dal.dataobject.ConversationWorkItemAuditDO;
import org.chovy.canvas.dal.dataobject.ConversationWorkItemDO;
import org.chovy.canvas.dal.mapper.ConversationRoutingAgentMapper;
import org.chovy.canvas.dal.mapper.ConversationRoutingRuleMapper;
import org.chovy.canvas.dal.mapper.ConversationSlaBreachMapper;
import org.chovy.canvas.dal.mapper.ConversationWorkItemAuditMapper;
import org.chovy.canvas.dal.mapper.ConversationWorkItemMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationRoutingServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T03:00:00Z"),
            ZoneId.of("Asia/Shanghai"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 6, 11, 0);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void upsertsAgentAndRuleWithNormalizedSkillsAndCapacity() throws Exception {
        Harness harness = harness();
        when(harness.agentMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(harness.ruleMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(harness.agentMapper.insert(any(ConversationRoutingAgentDO.class))).thenAnswer(invocation -> {
            invocation.<ConversationRoutingAgentDO>getArgument(0).setId(10L);
            return 1;
        });
        when(harness.ruleMapper.insert(any(ConversationRoutingRuleDO.class))).thenAnswer(invocation -> {
            invocation.<ConversationRoutingRuleDO>getArgument(0).setId(20L);
            return 1;
        });

        ConversationRoutingAgentView agent = harness.service.upsertAgent(7L,
                new ConversationRoutingAgentCommand(
                        "Alice",
                        "Alice",
                        "Sales",
                        "available",
                        3,
                        1,
                        List.of("Sales", "VIP"),
                        Map.of("region", "east")),
                "manager-1");
        ConversationRoutingRuleView rule = harness.service.upsertRule(7L,
                new ConversationRoutingRuleCommand(
                        "vip-sales",
                        "web_chat",
                        "high",
                        List.of("vip", "sales"),
                        "sales",
                        30,
                        true,
                        10,
                        Map.of("segment", "vip")),
                "manager-1");

        ArgumentCaptor<ConversationRoutingAgentDO> agentCaptor =
                ArgumentCaptor.forClass(ConversationRoutingAgentDO.class);
        ArgumentCaptor<ConversationRoutingRuleDO> ruleCaptor =
                ArgumentCaptor.forClass(ConversationRoutingRuleDO.class);
        verify(harness.agentMapper).insert(agentCaptor.capture());
        verify(harness.ruleMapper).insert(ruleCaptor.capture());
        assertThat(agent.agentKey()).isEqualTo("alice");
        assertThat(agent.skills()).containsExactly("sales", "vip");
        assertThat(agent.currentLoad()).isEqualTo(1);
        assertThat(readList(agentCaptor.getValue().getSkillsJson())).containsExactly("sales", "vip");
        assertThat(rule.ruleKey()).isEqualTo("vip-sales");
        assertThat(rule.requiredSkills()).containsExactly("vip", "sales");
        assertThat(rule.slaMinutes()).isEqualTo(30);
        assertThat(readList(ruleCaptor.getValue().getRequiredSkillsJson())).containsExactly("vip", "sales");
    }

    @Test
    void routeWorkItemChoosesLowestLoadAvailableAgentWithAllRequiredSkills() throws Exception {
        Harness harness = harness();
        when(harness.workItemMapper.selectById(400L)).thenReturn(workItem(400L, 7L, "OPEN", "HIGH"));
        when(harness.ruleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(rule()));
        when(harness.agentMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                agent("bob", "sales", "AVAILABLE", 3, 2, List.of("sales", "vip")),
                agent("alice", "sales", "AVAILABLE", 3, 1, List.of("sales", "vip")),
                agent("carl", "sales", "AVAILABLE", 3, 0, List.of("sales"))));

        ConversationRouteResultView result = harness.service.routeWorkItem(7L, 400L,
                new ConversationRouteCommand(List.of(), null, null, "VIP sales handoff"),
                "router-1");

        assertThat(result.routed()).isTrue();
        assertThat(result.assignedTo()).isEqualTo("alice");
        assertThat(result.assignedTeam()).isEqualTo("sales");
        assertThat(result.requiredSkills()).containsExactly("sales", "vip");
        assertThat(result.slaDueAt()).isEqualTo(NOW.plusMinutes(30));
        ArgumentCaptor<ConversationWorkItemDO> workItemCaptor =
                ArgumentCaptor.forClass(ConversationWorkItemDO.class);
        ArgumentCaptor<ConversationRoutingAgentDO> agentCaptor =
                ArgumentCaptor.forClass(ConversationRoutingAgentDO.class);
        ArgumentCaptor<ConversationWorkItemAuditDO> auditCaptor =
                ArgumentCaptor.forClass(ConversationWorkItemAuditDO.class);
        verify(harness.workItemMapper).updateById(workItemCaptor.capture());
        verify(harness.agentMapper).updateById(agentCaptor.capture());
        verify(harness.auditMapper).insert(auditCaptor.capture());
        assertThat(workItemCaptor.getValue().getAssignedTo()).isEqualTo("alice");
        assertThat(workItemCaptor.getValue().getRoutingStatus()).isEqualTo("ROUTED");
        assertThat(workItemCaptor.getValue().getSlaDueAt()).isEqualTo(NOW.plusMinutes(30));
        assertThat(workItemCaptor.getValue().getSlaPolicyKey()).isEqualTo("vip-sales");
        assertThat(readList(workItemCaptor.getValue().getRequiredSkillsJson())).containsExactly("sales", "vip");
        assertThat(agentCaptor.getValue().getId()).isEqualTo(100L);
        assertThat(agentCaptor.getValue().getCurrentLoad()).isEqualTo(2);
        assertThat(auditCaptor.getValue().getEventType()).isEqualTo("ROUTED");
        assertThat(auditCaptor.getValue().getActor()).isEqualTo("router-1");
    }

    @Test
    void routeWorkItemRecordsMissWhenNoAgentHasCapacity() {
        Harness harness = harness();
        when(harness.workItemMapper.selectById(400L)).thenReturn(workItem(400L, 7L, "OPEN", "HIGH"));
        when(harness.ruleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(rule()));
        when(harness.agentMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                agent("alice", "sales", "AVAILABLE", 1, 1, List.of("sales", "vip")),
                agent("bob", "sales", "OFFLINE", 3, 0, List.of("sales", "vip"))));

        ConversationRouteResultView result = harness.service.routeWorkItem(7L, 400L,
                new ConversationRouteCommand(List.of("sales", "vip"), "sales", 30, "route now"),
                "router-1");

        assertThat(result.routed()).isFalse();
        assertThat(result.routingStatus()).isEqualTo("UNROUTED");
        assertThat(result.routingReason()).contains("no available agent");
        ArgumentCaptor<ConversationWorkItemDO> workItemCaptor =
                ArgumentCaptor.forClass(ConversationWorkItemDO.class);
        ArgumentCaptor<ConversationWorkItemAuditDO> auditCaptor =
                ArgumentCaptor.forClass(ConversationWorkItemAuditDO.class);
        verify(harness.workItemMapper).updateById(workItemCaptor.capture());
        verify(harness.agentMapper, never()).updateById(any(ConversationRoutingAgentDO.class));
        verify(harness.auditMapper).insert(auditCaptor.capture());
        assertThat(workItemCaptor.getValue().getAssignedTo()).isNull();
        assertThat(workItemCaptor.getValue().getRoutingStatus()).isEqualTo("UNROUTED");
        assertThat(auditCaptor.getValue().getEventType()).isEqualTo("ROUTING_MISSED");
    }

    @Test
    void evaluateSlaBreachesCreatesOpenBreachesIdempotentlyAndRaisesPriority() {
        Harness harness = harness();
        ConversationWorkItemDO due = workItem(400L, 7L, "OPEN", "NORMAL");
        due.setSlaDueAt(NOW.minusMinutes(1));
        ConversationWorkItemDO existingDue = workItem(401L, 7L, "PENDING", "HIGH");
        existingDue.setSlaDueAt(NOW.minusMinutes(5));
        when(harness.workItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(due, existingDue));
        when(harness.breachMapper.selectOne(any(Wrapper.class)))
                .thenReturn(null)
                .thenReturn(breach(900L, 7L, 401L));
        when(harness.breachMapper.insert(any(ConversationSlaBreachDO.class))).thenAnswer(invocation -> {
            invocation.<ConversationSlaBreachDO>getArgument(0).setId(901L);
            return 1;
        });

        ConversationSlaEvaluationView result =
                harness.service.evaluateSlaBreaches(7L, NOW, "scheduler", 100);

        assertThat(result.scanned()).isEqualTo(2);
        assertThat(result.created()).isEqualTo(1);
        assertThat(result.skippedExisting()).isEqualTo(1);
        ArgumentCaptor<ConversationSlaBreachDO> breachCaptor =
                ArgumentCaptor.forClass(ConversationSlaBreachDO.class);
        ArgumentCaptor<ConversationWorkItemDO> workItemCaptor =
                ArgumentCaptor.forClass(ConversationWorkItemDO.class);
        ArgumentCaptor<ConversationWorkItemAuditDO> auditCaptor =
                ArgumentCaptor.forClass(ConversationWorkItemAuditDO.class);
        verify(harness.breachMapper).insert(breachCaptor.capture());
        verify(harness.workItemMapper).updateById(workItemCaptor.capture());
        verify(harness.auditMapper).insert(auditCaptor.capture());
        assertThat(breachCaptor.getValue().getWorkItemId()).isEqualTo(400L);
        assertThat(breachCaptor.getValue().getStatus()).isEqualTo("OPEN");
        assertThat(workItemCaptor.getValue().getPriority()).isEqualTo("HIGH");
        assertThat(auditCaptor.getValue().getEventType()).isEqualTo("SLA_BREACHED");
    }

    @Test
    void rejectsCrossTenantWorkItem() {
        Harness harness = harness();
        when(harness.workItemMapper.selectById(400L)).thenReturn(workItem(400L, 99L, "OPEN", "HIGH"));

        assertThatThrownBy(() -> harness.service.routeWorkItem(7L, 400L,
                new ConversationRouteCommand(List.of(), null, null, null),
                "router-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Conversation work item not found");
    }

    private Harness harness() {
        ConversationWorkItemMapper workItemMapper = mock(ConversationWorkItemMapper.class);
        ConversationWorkItemAuditMapper auditMapper = mock(ConversationWorkItemAuditMapper.class);
        ConversationRoutingAgentMapper agentMapper = mock(ConversationRoutingAgentMapper.class);
        ConversationRoutingRuleMapper ruleMapper = mock(ConversationRoutingRuleMapper.class);
        ConversationSlaBreachMapper breachMapper = mock(ConversationSlaBreachMapper.class);
        ConversationRoutingService service = new ConversationRoutingService(
                workItemMapper,
                auditMapper,
                agentMapper,
                ruleMapper,
                breachMapper,
                OBJECT_MAPPER,
                CLOCK);
        return new Harness(service, workItemMapper, auditMapper, agentMapper, ruleMapper, breachMapper);
    }

    private ConversationWorkItemDO workItem(Long id, Long tenantId, String status, String priority) {
        ConversationWorkItemDO row = new ConversationWorkItemDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setSessionId(100L);
        row.setContactProfileId(300L);
        row.setUserId("user-1");
        row.setChannel("WEB_CHAT");
        row.setProvider("WIDGET");
        row.setSubject("WEB_CHAT conversation with user-1");
        row.setStatus(status);
        row.setPriority(priority);
        row.setAssignedTo(null);
        row.setAssignedTeam(null);
        row.setSource("CONVERSATION");
        row.setTagsJson("[]");
        row.setAttributesJson("{\"segment\":\"vip\"}");
        row.setLastCustomerMessageAt(NOW.minusMinutes(10));
        row.setCreatedAt(NOW.minusMinutes(20));
        row.setUpdatedAt(NOW.minusMinutes(10));
        return row;
    }

    private ConversationRoutingRuleDO rule() {
        ConversationRoutingRuleDO row = new ConversationRoutingRuleDO();
        row.setId(20L);
        row.setTenantId(7L);
        row.setRuleKey("vip-sales");
        row.setChannel("WEB_CHAT");
        row.setMinPriority("HIGH");
        row.setRequiredSkillsJson("[\"sales\",\"vip\"]");
        row.setTargetTeam("sales");
        row.setSlaMinutes(30);
        row.setEnabled(1);
        row.setSortOrder(10);
        row.setMetadataJson("{}");
        row.setCreatedAt(NOW.minusDays(1));
        row.setUpdatedAt(NOW.minusDays(1));
        return row;
    }

    private ConversationRoutingAgentDO agent(String key,
                                             String team,
                                             String status,
                                             int maxCapacity,
                                             int currentLoad,
                                             List<String> skills) {
        ConversationRoutingAgentDO row = new ConversationRoutingAgentDO();
        row.setId("alice".equals(key) ? 100L : 101L);
        row.setTenantId(7L);
        row.setAgentKey(key);
        row.setDisplayName(key);
        row.setTeamKey(team);
        row.setStatus(status);
        row.setMaxCapacity(maxCapacity);
        row.setCurrentLoad(currentLoad);
        row.setSkillsJson(writeJson(skills));
        row.setMetadataJson("{}");
        row.setCreatedAt(NOW.minusDays(1));
        row.setUpdatedAt(NOW.minusDays(1));
        return row;
    }

    private ConversationSlaBreachDO breach(Long id, Long tenantId, Long workItemId) {
        ConversationSlaBreachDO row = new ConversationSlaBreachDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setWorkItemId(workItemId);
        row.setBreachType("FIRST_RESPONSE");
        row.setSeverity("HIGH");
        row.setStatus("OPEN");
        row.setDueAt(NOW.minusMinutes(5));
        row.setBreachedAt(NOW.minusMinutes(5));
        row.setCreatedAt(NOW.minusMinutes(5));
        row.setUpdatedAt(NOW.minusMinutes(5));
        return row;
    }

    private String writeJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private static List<String> readList(String json) throws Exception {
        return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
    }

    private record Harness(ConversationRoutingService service,
                           ConversationWorkItemMapper workItemMapper,
                           ConversationWorkItemAuditMapper auditMapper,
                           ConversationRoutingAgentMapper agentMapper,
                           ConversationRoutingRuleMapper ruleMapper,
                           ConversationSlaBreachMapper breachMapper) {
    }
}
