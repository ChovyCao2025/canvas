package org.chovy.canvas.conversation.adapter.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import org.chovy.canvas.conversation.domain.port.ConversationWorkItemAuditRepository;
import org.chovy.canvas.conversation.domain.port.ConversationWorkItemRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public final class MybatisConversationRepository {

    private MybatisConversationRepository() {
    }
}

@Repository
class MybatisConversationSessionRepository implements ConversationSessionRepository {

    private final ConversationSessionMapper mapper;

    MybatisConversationSessionRepository(ConversationSessionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<ConversationSession> findActive(Long tenantId,
                                                    String userId,
                                                    String channel,
                                                    String provider,
                                                    String executionId) {
        QueryWrapper<ConversationSessionDO> query = new QueryWrapper<ConversationSessionDO>()
                .eq("tenant_id", tenantId)
                .eq("user_id", userId)
                .eq("channel", channel)
                .eq("provider", provider)
                .eq("status", "ACTIVE")
                .eq(executionId != null, "execution_id", executionId)
                .isNull(executionId == null, "execution_id")
                .orderByDesc("last_message_at")
                .last("LIMIT 1");
        return Optional.ofNullable(mapper.selectOne(query))
                .map(ConversationPersistenceConverter::toSession);
    }

    @Override
    public Optional<ConversationSession> byId(Long id) {
        return Optional.ofNullable(mapper.selectById(id))
                .map(ConversationPersistenceConverter::toSession);
    }

    @Override
    public ConversationSession save(ConversationSession session) {
        ConversationSessionDO row = ConversationPersistenceConverter.toSessionRow(session);
        if (row.id == null) {
            mapper.insert(row);
        } else {
            mapper.updateById(row);
        }
        return ConversationPersistenceConverter.toSession(row);
    }
}

@Repository
class MybatisConversationMessageRepository implements ConversationMessageRepository {

    private final ConversationMessageMapper mapper;

    MybatisConversationMessageRepository(ConversationMessageMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<ConversationMessage> byIdempotencyKey(Long tenantId, String idempotencyKey) {
        return Optional.ofNullable(mapper.selectOne(new QueryWrapper<ConversationMessageDO>()
                        .eq("tenant_id", tenantId)
                        .eq("idempotency_key", idempotencyKey)
                        .last("LIMIT 1")))
                .map(ConversationPersistenceConverter::toMessage);
    }

    @Override
    public ConversationMessage save(ConversationMessage message) {
        ConversationMessageDO row = ConversationPersistenceConverter.toMessageRow(message);
        if (row.id == null) {
            mapper.insert(row);
        } else {
            mapper.updateById(row);
        }
        return ConversationPersistenceConverter.toMessage(row);
    }
}

@Repository
class MybatisConversationContactProfileRepository implements ConversationContactProfileRepository {

    private final ConversationContactProfileMapper mapper;

    MybatisConversationContactProfileRepository(ConversationContactProfileMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<ConversationContactProfile> byUser(Long tenantId, String userId) {
        return Optional.ofNullable(mapper.selectOne(new QueryWrapper<ConversationContactProfileDO>()
                        .eq("tenant_id", tenantId)
                        .eq("user_id", userId)
                        .last("LIMIT 1")))
                .map(ConversationPersistenceConverter::toContactProfile);
    }

    @Override
    public ConversationContactProfile save(ConversationContactProfile profile) {
        ConversationContactProfileDO row = ConversationPersistenceConverter.toContactProfileRow(profile);
        if (row.id == null) {
            mapper.insert(row);
        } else {
            mapper.updateById(row);
        }
        return ConversationPersistenceConverter.toContactProfile(row);
    }
}

@Repository
class MybatisConversationWorkItemRepository implements ConversationWorkItemRepository {

    private final ConversationWorkItemMapper mapper;

    MybatisConversationWorkItemRepository(ConversationWorkItemMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<ConversationWorkItem> bySession(Long tenantId, Long sessionId) {
        return Optional.ofNullable(mapper.selectOne(new QueryWrapper<ConversationWorkItemDO>()
                        .eq("tenant_id", tenantId)
                        .eq("session_id", sessionId)
                        .last("LIMIT 1")))
                .map(ConversationPersistenceConverter::toWorkItem);
    }

    @Override
    public Optional<ConversationWorkItem> byId(Long id) {
        return Optional.ofNullable(mapper.selectById(id))
                .map(ConversationPersistenceConverter::toWorkItem);
    }

    @Override
    public List<ConversationWorkItem> dueForSla(Long tenantId, LocalDateTime now, int limit) {
        return mapper.selectList(new QueryWrapper<ConversationWorkItemDO>()
                        .eq("tenant_id", tenantId)
                        .in("status", List.of("OPEN", "PENDING", "SNOOZED"))
                        .isNotNull("sla_due_at")
                        .le("sla_due_at", now)
                        .orderByAsc("sla_due_at")
                        .last("LIMIT " + Math.max(1, limit)))
                .stream()
                .map(ConversationPersistenceConverter::toWorkItem)
                .toList();
    }

    @Override
    public ConversationWorkItem save(ConversationWorkItem workItem) {
        ConversationWorkItemDO row = ConversationPersistenceConverter.toWorkItemRow(workItem);
        if (row.id == null) {
            mapper.insert(row);
        } else {
            mapper.updateById(row);
        }
        return ConversationPersistenceConverter.toWorkItem(row);
    }
}

@Repository
class MybatisConversationWorkItemAuditRepository implements ConversationWorkItemAuditRepository {

    private final ConversationWorkItemAuditMapper mapper;

    MybatisConversationWorkItemAuditRepository(ConversationWorkItemAuditMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ConversationWorkItemAudit save(ConversationWorkItemAudit audit) {
        ConversationWorkItemAuditDO row = ConversationPersistenceConverter.toWorkItemAuditRow(audit);
        if (row.id == null) {
            mapper.insert(row);
        } else {
            mapper.updateById(row);
        }
        return ConversationPersistenceConverter.toWorkItemAudit(row);
    }
}

@Repository
class MybatisConversationRoutingAgentRepository implements ConversationRoutingAgentRepository {

    private final ConversationRoutingAgentMapper mapper;

    MybatisConversationRoutingAgentRepository(ConversationRoutingAgentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<ConversationRoutingAgent> byKey(Long tenantId, String agentKey) {
        return Optional.ofNullable(mapper.selectOne(new QueryWrapper<ConversationRoutingAgentDO>()
                        .eq("tenant_id", tenantId)
                        .eq("agent_key", agentKey)
                        .last("LIMIT 1")))
                .map(ConversationPersistenceConverter::toRoutingAgent);
    }

    @Override
    public List<ConversationRoutingAgent> candidates(Long tenantId, String teamKey) {
        return mapper.selectList(new QueryWrapper<ConversationRoutingAgentDO>()
                        .eq("tenant_id", tenantId)
                        .eq("status", "AVAILABLE")
                        .eq(teamKey != null, "team_key", teamKey)
                        .orderByAsc("current_load")
                        .orderByAsc("agent_key"))
                .stream()
                .map(ConversationPersistenceConverter::toRoutingAgent)
                .toList();
    }

    @Override
    public ConversationRoutingAgent save(ConversationRoutingAgent agent) {
        ConversationRoutingAgentDO row = ConversationPersistenceConverter.toRoutingAgentRow(agent);
        if (row.id == null) {
            mapper.insert(row);
        } else {
            mapper.updateById(row);
        }
        return ConversationPersistenceConverter.toRoutingAgent(row);
    }
}

@Repository
class MybatisConversationRoutingRuleRepository implements ConversationRoutingRuleRepository {

    private final ConversationRoutingRuleMapper mapper;

    MybatisConversationRoutingRuleRepository(ConversationRoutingRuleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<ConversationRoutingRule> byKey(Long tenantId, String ruleKey) {
        return Optional.ofNullable(mapper.selectOne(new QueryWrapper<ConversationRoutingRuleDO>()
                        .eq("tenant_id", tenantId)
                        .eq("rule_key", ruleKey)
                        .last("LIMIT 1")))
                .map(ConversationPersistenceConverter::toRoutingRule);
    }

    @Override
    public List<ConversationRoutingRule> enabled(Long tenantId) {
        return mapper.selectList(new QueryWrapper<ConversationRoutingRuleDO>()
                        .eq("tenant_id", tenantId)
                        .eq("enabled", 1)
                        .orderByAsc("sort_order")
                        .orderByAsc("rule_key"))
                .stream()
                .map(ConversationPersistenceConverter::toRoutingRule)
                .toList();
    }

    @Override
    public ConversationRoutingRule save(ConversationRoutingRule rule) {
        ConversationRoutingRuleDO row = ConversationPersistenceConverter.toRoutingRuleRow(rule);
        if (row.id == null) {
            mapper.insert(row);
        } else {
            mapper.updateById(row);
        }
        return ConversationPersistenceConverter.toRoutingRule(row);
    }
}

@Repository
class MybatisConversationSlaBreachRepository implements ConversationSlaBreachRepository {

    private final ConversationSlaBreachMapper mapper;

    MybatisConversationSlaBreachRepository(ConversationSlaBreachMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<ConversationSlaBreach> openByWorkItem(Long tenantId, Long workItemId) {
        return Optional.ofNullable(mapper.selectOne(new QueryWrapper<ConversationSlaBreachDO>()
                        .eq("tenant_id", tenantId)
                        .eq("work_item_id", workItemId)
                        .eq("status", "OPEN")
                        .orderByDesc("breached_at")
                        .last("LIMIT 1")))
                .map(ConversationPersistenceConverter::toSlaBreach);
    }

    @Override
    public ConversationSlaBreach save(ConversationSlaBreach breach) {
        ConversationSlaBreachDO row = ConversationPersistenceConverter.toSlaBreachRow(breach);
        if (row.id == null) {
            mapper.insert(row);
        } else {
            mapper.updateById(row);
        }
        return ConversationPersistenceConverter.toSlaBreach(row);
    }
}
