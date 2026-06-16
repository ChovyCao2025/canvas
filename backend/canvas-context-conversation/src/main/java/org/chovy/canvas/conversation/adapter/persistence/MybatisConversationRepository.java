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

/**
 * 聚合 MyBatis 仓储实现的占位类型，防止工具类被实例化。
 */
public final class MybatisConversationRepository {

    /**
     * 禁止实例化聚合占位类型。
     */
    private MybatisConversationRepository() {
    }
}

/**
 * 基于 MyBatis Plus 的会话仓储实现。
 */
@Repository
class MybatisConversationSessionRepository implements ConversationSessionRepository {

    /**
     * 会话表访问器。
     */
    private final ConversationSessionMapper mapper;

    /**
     * 创建会话仓储。
     *
     * @param mapper 会话表访问器
     */
    MybatisConversationSessionRepository(ConversationSessionMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 查询租户内指定用户和渠道的最新活跃会话。
     *
     * @param tenantId 租户标识
     * @param userId 用户标识
     * @param channel 渠道
     * @param provider 服务商
     * @param executionId 外部执行标识
     * @return 最新活跃会话
     */
    @Override
    public Optional<ConversationSession> findActive(Long tenantId,
                                                    String userId,
                                                    String channel,
                                                    String provider,
                                                    String executionId) {
        // executionId 为空时显式匹配 NULL，避免同一用户跨执行链路误复用会话。
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

    /**
     * 按主键查询会话。
     *
     * @param id 会话主键
     * @return 匹配会话
     */
    @Override
    public Optional<ConversationSession> byId(Long id) {
        return Optional.ofNullable(mapper.selectById(id))
                .map(ConversationPersistenceConverter::toSession);
    }

    /**
     * 保存会话，主键为空时插入，否则更新。
     *
     * @param session 待保存会话
     * @return 保存后的会话
     */
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

/**
 * 基于 MyBatis Plus 的会话消息仓储实现。
 */
@Repository
class MybatisConversationMessageRepository implements ConversationMessageRepository {

    /**
     * 会话消息表访问器。
     */
    private final ConversationMessageMapper mapper;

    /**
     * 创建消息仓储。
     *
     * @param mapper 会话消息表访问器
     */
    MybatisConversationMessageRepository(ConversationMessageMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 按租户和幂等键查找已存在消息。
     *
     * @param tenantId 租户标识
     * @param idempotencyKey 消息幂等键
     * @return 已存在消息
     */
    @Override
    public Optional<ConversationMessage> byIdempotencyKey(Long tenantId, String idempotencyKey) {
        return Optional.ofNullable(mapper.selectOne(new QueryWrapper<ConversationMessageDO>()
                        .eq("tenant_id", tenantId)
                        .eq("idempotency_key", idempotencyKey)
                        .last("LIMIT 1")))
                .map(ConversationPersistenceConverter::toMessage);
    }

    /**
     * 保存消息，主键为空时插入，否则更新。
     *
     * @param message 待保存消息
     * @return 保存后的消息
     */
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

/**
 * 基于 MyBatis Plus 的联系人画像仓储实现。
 */
@Repository
class MybatisConversationContactProfileRepository implements ConversationContactProfileRepository {

    /**
     * 联系人画像表访问器。
     */
    private final ConversationContactProfileMapper mapper;

    /**
     * 创建联系人画像仓储。
     *
     * @param mapper 联系人画像表访问器
     */
    MybatisConversationContactProfileRepository(ConversationContactProfileMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 按租户和用户标识查找联系人画像。
     *
     * @param tenantId 租户标识
     * @param userId 用户标识
     * @return 匹配的联系人画像
     */
    @Override
    public Optional<ConversationContactProfile> byUser(Long tenantId, String userId) {
        return Optional.ofNullable(mapper.selectOne(new QueryWrapper<ConversationContactProfileDO>()
                        .eq("tenant_id", tenantId)
                        .eq("user_id", userId)
                        .last("LIMIT 1")))
                .map(ConversationPersistenceConverter::toContactProfile);
    }

    /**
     * 保存联系人画像，主键为空时插入，否则更新。
     *
     * @param profile 待保存联系人画像
     * @return 保存后的联系人画像
     */
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

/**
 * 基于 MyBatis Plus 的会话工单仓储实现。
 */
@Repository
class MybatisConversationWorkItemRepository implements ConversationWorkItemRepository {

    /**
     * 会话工单表访问器。
     */
    private final ConversationWorkItemMapper mapper;

    /**
     * 创建工单仓储。
     *
     * @param mapper 会话工单表访问器
     */
    MybatisConversationWorkItemRepository(ConversationWorkItemMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 按租户和会话查找工单。
     *
     * @param tenantId 租户标识
     * @param sessionId 会话标识
     * @return 匹配工单
     */
    @Override
    public Optional<ConversationWorkItem> bySession(Long tenantId, Long sessionId) {
        return Optional.ofNullable(mapper.selectOne(new QueryWrapper<ConversationWorkItemDO>()
                        .eq("tenant_id", tenantId)
                        .eq("session_id", sessionId)
                        .last("LIMIT 1")))
                .map(ConversationPersistenceConverter::toWorkItem);
    }

    /**
     * 按主键查询工单。
     *
     * @param id 工单主键
     * @return 匹配工单
     */
    @Override
    public Optional<ConversationWorkItem> byId(Long id) {
        return Optional.ofNullable(mapper.selectById(id))
                .map(ConversationPersistenceConverter::toWorkItem);
    }

    /**
     * 查询已到 SLA 检查时间的未完结工单。
     *
     * @param tenantId 租户标识
     * @param now 检查基准时间
     * @param limit 返回数量上限
     * @return 待 SLA 检查工单列表
     */
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

    /**
     * 保存工单，主键为空时插入，否则更新。
     *
     * @param workItem 待保存工单
     * @return 保存后的工单
     */
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

/**
 * 基于 MyBatis Plus 的工单审计仓储实现。
 */
@Repository
class MybatisConversationWorkItemAuditRepository implements ConversationWorkItemAuditRepository {

    /**
     * 工单审计表访问器。
     */
    private final ConversationWorkItemAuditMapper mapper;

    /**
     * 创建工单审计仓储。
     *
     * @param mapper 工单审计表访问器
     */
    MybatisConversationWorkItemAuditRepository(ConversationWorkItemAuditMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 保存工单审计事件，主键为空时插入，否则更新。
     *
     * @param audit 待保存审计事件
     * @return 保存后的审计事件
     */
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

/**
 * 基于 MyBatis Plus 的路由坐席仓储实现。
 */
@Repository
class MybatisConversationRoutingAgentRepository implements ConversationRoutingAgentRepository {

    /**
     * 路由坐席表访问器。
     */
    private final ConversationRoutingAgentMapper mapper;

    /**
     * 创建路由坐席仓储。
     *
     * @param mapper 路由坐席表访问器
     */
    MybatisConversationRoutingAgentRepository(ConversationRoutingAgentMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 按租户和坐席业务键查询坐席。
     *
     * @param tenantId 租户标识
     * @param agentKey 坐席业务键
     * @return 匹配坐席
     */
    @Override
    public Optional<ConversationRoutingAgent> byKey(Long tenantId, String agentKey) {
        return Optional.ofNullable(mapper.selectOne(new QueryWrapper<ConversationRoutingAgentDO>()
                        .eq("tenant_id", tenantId)
                        .eq("agent_key", agentKey)
                        .last("LIMIT 1")))
                .map(ConversationPersistenceConverter::toRoutingAgent);
    }

    /**
     * 查询可参与路由的坐席候选集。
     *
     * @param tenantId 租户标识
     * @param teamKey 目标团队键
     * @return 候选坐席列表
     */
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

    /**
     * 保存路由坐席，主键为空时插入，否则更新。
     *
     * @param agent 待保存坐席
     * @return 保存后的坐席
     */
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

/**
 * 基于 MyBatis Plus 的路由规则仓储实现。
 */
@Repository
class MybatisConversationRoutingRuleRepository implements ConversationRoutingRuleRepository {

    /**
     * 路由规则表访问器。
     */
    private final ConversationRoutingRuleMapper mapper;

    /**
     * 创建路由规则仓储。
     *
     * @param mapper 路由规则表访问器
     */
    MybatisConversationRoutingRuleRepository(ConversationRoutingRuleMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 按租户和规则业务键查询规则。
     *
     * @param tenantId 租户标识
     * @param ruleKey 规则业务键
     * @return 匹配规则
     */
    @Override
    public Optional<ConversationRoutingRule> byKey(Long tenantId, String ruleKey) {
        return Optional.ofNullable(mapper.selectOne(new QueryWrapper<ConversationRoutingRuleDO>()
                        .eq("tenant_id", tenantId)
                        .eq("rule_key", ruleKey)
                        .last("LIMIT 1")))
                .map(ConversationPersistenceConverter::toRoutingRule);
    }

    /**
     * 查询租户内启用的路由规则。
     *
     * @param tenantId 租户标识
     * @return 已启用规则列表
     */
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

    /**
     * 保存路由规则，主键为空时插入，否则更新。
     *
     * @param rule 待保存规则
     * @return 保存后的规则
     */
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

/**
 * 基于 MyBatis Plus 的 SLA 违约仓储实现。
 */
@Repository
class MybatisConversationSlaBreachRepository implements ConversationSlaBreachRepository {

    /**
     * SLA 违约表访问器。
     */
    private final ConversationSlaBreachMapper mapper;

    /**
     * 创建 SLA 违约仓储。
     *
     * @param mapper SLA 违约表访问器
     */
    MybatisConversationSlaBreachRepository(ConversationSlaBreachMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 查询工单当前打开的 SLA 违约记录。
     *
     * @param tenantId 租户标识
     * @param workItemId 工单标识
     * @return 最新打开的 SLA 违约记录
     */
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

    /**
     * 保存 SLA 违约记录，主键为空时插入，否则更新。
     *
     * @param breach 待保存违约记录
     * @return 保存后的违约记录
     */
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
