package org.chovy.canvas.domain.conversation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * ConversationRoutingService 编排 domain.conversation 场景的领域业务规则。
 */
@Service
public class ConversationRoutingService {

    private static final Set<String> AGENT_STATUSES = Set.of("AVAILABLE", "BUSY", "OFFLINE");
    private static final Set<String> PRIORITIES = Set.of("LOW", "NORMAL", "HIGH", "URGENT");
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ConversationWorkItemMapper workItemMapper;
    private final ConversationWorkItemAuditMapper auditMapper;
    private final ConversationRoutingAgentMapper agentMapper;
    private final ConversationRoutingRuleMapper ruleMapper;
    private final ConversationSlaBreachMapper breachMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 创建 ConversationRoutingService 实例并注入 domain.conversation 场景依赖。
     * @param workItemMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param agentMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param ruleMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param breachMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public ConversationRoutingService(ConversationWorkItemMapper workItemMapper,
                                      ConversationWorkItemAuditMapper auditMapper,
                                      ConversationRoutingAgentMapper agentMapper,
                                      ConversationRoutingRuleMapper ruleMapper,
                                      ConversationSlaBreachMapper breachMapper,
                                      ObjectMapper objectMapper) {
        this(workItemMapper, auditMapper, agentMapper, ruleMapper, breachMapper,
                objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 执行 ConversationRoutingService 流程，围绕 conversation routing service 完成校验、计算或结果组装。
     *
     * @param workItemMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param agentMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param ruleMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param breachMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    ConversationRoutingService(ConversationWorkItemMapper workItemMapper,
                               ConversationWorkItemAuditMapper auditMapper,
                               ConversationRoutingAgentMapper agentMapper,
                               ConversationRoutingRuleMapper ruleMapper,
                               ConversationSlaBreachMapper breachMapper,
                               ObjectMapper objectMapper,
                               Clock clock) {
        this.workItemMapper = workItemMapper;
        this.auditMapper = auditMapper;
        this.agentMapper = agentMapper;
        this.ruleMapper = ruleMapper;
        this.breachMapper = breachMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 新增或更新租户内客服路由坐席。
     * 会规范化坐席状态、团队、技能和容量，写入当前负载，并返回可路由视图。
     */
    public ConversationRoutingAgentView upsertAgent(Long tenantId,
                                                    ConversationRoutingAgentCommand command,
                                                    String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("conversation routing agent command is required");
        }
        Long scopedTenantId = tenantId(tenantId);
        String agentKey = key(command.agentKey(), "agentKey");
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ConversationRoutingAgentDO row = agentMapper.selectOne(new LambdaQueryWrapper<ConversationRoutingAgentDO>()
                .eq(ConversationRoutingAgentDO::getTenantId, scopedTenantId)
                .eq(ConversationRoutingAgentDO::getAgentKey, agentKey)
                .last("LIMIT 1"));
        if (row == null) {
            row = new ConversationRoutingAgentDO();
            row.setTenantId(scopedTenantId);
            row.setAgentKey(agentKey);
            row.setCreatedBy(actor(actor));
            row.setCreatedAt(now());
        }
        int maxCapacity = positive(command.maxCapacity(), 1);
        int currentLoad = Math.max(0, Math.min(value(command.currentLoad()), maxCapacity));
        row.setDisplayName(defaultString(command.displayName(), agentKey));
        row.setTeamKey(optionalKey(command.teamKey()));
        row.setStatus(normalizeAgentStatus(command.status()));
        row.setMaxCapacity(maxCapacity);
        row.setCurrentLoad(currentLoad);
        row.setSkillsJson(writeJson(normalizedKeys(command.skills())));
        row.setMetadataJson(writeJson(command.metadata()));
        row.setUpdatedAt(now());
        if (row.getId() == null) {
            agentMapper.insert(row);
        } else {
            agentMapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toAgentView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 新增或更新租户内会话路由规则。
     * 规则包含渠道、最低优先级、所需技能、目标团队和 SLA 分钟数，后续工单路由会按排序优先匹配启用规则。
     */
    public ConversationRoutingRuleView upsertRule(Long tenantId,
                                                  ConversationRoutingRuleCommand command,
                                                  String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("conversation routing rule command is required");
        }
        Long scopedTenantId = tenantId(tenantId);
        String ruleKey = key(command.ruleKey(), "ruleKey");
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ConversationRoutingRuleDO row = ruleMapper.selectOne(new LambdaQueryWrapper<ConversationRoutingRuleDO>()
                .eq(ConversationRoutingRuleDO::getTenantId, scopedTenantId)
                .eq(ConversationRoutingRuleDO::getRuleKey, ruleKey)
                .last("LIMIT 1"));
        if (row == null) {
            row = new ConversationRoutingRuleDO();
            row.setTenantId(scopedTenantId);
            row.setRuleKey(ruleKey);
            row.setCreatedBy(actor(actor));
            row.setCreatedAt(now());
        }
        row.setChannel(optionalUpper(command.channel()));
        row.setMinPriority(normalizePriority(defaultString(command.minPriority(), "NORMAL")));
        row.setRequiredSkillsJson(writeJson(normalizedKeys(command.requiredSkills())));
        row.setTargetTeam(optionalKey(command.targetTeam()));
        row.setSlaMinutes(positive(command.slaMinutes(), 60));
        row.setEnabled(Boolean.FALSE.equals(command.enabled()) ? 0 : 1);
        row.setSortOrder(command.sortOrder() == null ? 1000 : command.sortOrder());
        row.setMetadataJson(writeJson(command.metadata()));
        row.setUpdatedAt(now());
        if (row.getId() == null) {
            ruleMapper.insert(row);
        } else {
            ruleMapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toRuleView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 为会话工单匹配路由规则并分配可用坐席。
     * 成功时会更新工单分配人、团队、SLA 到期时间和坐席负载，并写入审计；无可用坐席时只标记 UNROUTED。
     */
    public ConversationRouteResultView routeWorkItem(Long tenantId,
                                                     Long workItemId,
                                                     ConversationRouteCommand command,
                                                     String actor) {
        Long scopedTenantId = tenantId(tenantId);
        ConversationWorkItemDO workItem = requireWorkItem(scopedTenantId, workItemId);
        if ("RESOLVED".equals(normalizeOptionalUpper(workItem.getStatus()))) {
            throw new IllegalStateException("resolved work item cannot be routed");
        }
        ConversationRoutingRuleDO rule = matchingRule(scopedTenantId, workItem);
        List<String> requiredSkills = command != null && !command.requiredSkills().isEmpty()
                /**
                 * 规范化输入值。
                 *
                 * @return 返回解析、归一化或安全处理后的值。
                 */
                ? normalizedKeys(command.requiredSkills())
                : rule == null ? List.of() : stringList(rule.getRequiredSkillsJson());
        String targetTeam = command == null || isBlank(command.targetTeam())
                ? rule == null ? null : rule.getTargetTeam()
                /**
                 * 执行 optionalKey 流程，围绕 optional key 完成校验、计算或结果组装。
                 *
                 * @return 返回 optionalKey 流程生成的业务结果。
                 */
                : optionalKey(command.targetTeam());
        int slaMinutes = command == null || command.slaMinutes() == null
                ? rule == null ? 60 : positive(rule.getSlaMinutes(), 60)
                /**
                 * 执行 positive 流程，围绕 positive 完成校验、计算或结果组装。
                 *
                 * @return 返回 positive 流程生成的业务结果。
                 */
                : positive(command.slaMinutes(), rule == null ? 60 : positive(rule.getSlaMinutes(), 60));
        ConversationRoutingAgentDO agent = bestAgent(scopedTenantId, targetTeam, requiredSkills);
        if (agent == null) {
            return routeMiss(workItem, requiredSkills, "no available agent for required skills", actor);
        }
        LocalDateTime routedAt = now();
        LocalDateTime slaDueAt = routedAt.plusMinutes(slaMinutes);
        ConversationWorkItemDO update = new ConversationWorkItemDO();
        update.setId(workItem.getId());
        update.setAssignedTo(agent.getAgentKey());
        update.setAssignedTeam(agent.getTeamKey());
        update.setSlaDueAt(slaDueAt);
        update.setRoutingStatus("ROUTED");
        update.setRequiredSkillsJson(writeJson(requiredSkills));
        update.setRoutingReason("matched rule " + (rule == null ? "default" : rule.getRuleKey())
                + " to agent " + agent.getAgentKey());
        update.setRoutedAt(routedAt);
        update.setSlaPolicyKey(rule == null ? null : rule.getRuleKey());
        update.setUpdatedAt(routedAt);
        workItemMapper.updateById(update);

        ConversationRoutingAgentDO agentUpdate = new ConversationRoutingAgentDO();
        agentUpdate.setId(agent.getId());
        agentUpdate.setCurrentLoad(value(agent.getCurrentLoad()) + 1);
        agentUpdate.setUpdatedAt(routedAt);
        agentMapper.updateById(agentUpdate);

        audit(workItem.getTenantId(), workItem.getId(), "ROUTED", actor,
                values("assignedTo", workItem.getAssignedTo(), "assignedTeam", workItem.getAssignedTeam()),
                values("assignedTo", agent.getAgentKey(), "assignedTeam", agent.getTeamKey(),
                        "requiredSkills", requiredSkills, "slaDueAt", slaDueAt),
                command == null ? null : command.note());
        return new ConversationRouteResultView(
                workItem.getTenantId(),
                workItem.getId(),
                true,
                agent.getAgentKey(),
                agent.getTeamKey(),
                "ROUTED",
                update.getRoutingReason(),
                requiredSkills,
                slaDueAt);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 扫描租户内已超过 SLA 的未解决工单并生成违约记录。
     * 已存在 OPEN 违约会被跳过，新违约会提升低优先级工单到 HIGH 并写入工单审计。
     */
    public ConversationSlaEvaluationView evaluateSlaBreaches(Long tenantId,
                                                             LocalDateTime evaluatedAt,
                                                             String actor,
                                                             int limit) {
        Long scopedTenantId = tenantId(tenantId);
        LocalDateTime effectiveNow = evaluatedAt == null ? now() : evaluatedAt;
        int boundedLimit = boundedLimit(limit);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<ConversationWorkItemDO> rows = safeList(workItemMapper.selectList(new LambdaQueryWrapper<ConversationWorkItemDO>()
                .eq(ConversationWorkItemDO::getTenantId, scopedTenantId)
                .le(ConversationWorkItemDO::getSlaDueAt, effectiveNow)
                .last("LIMIT " + boundedLimit)));
        int scanned = 0;
        int skippedExisting = 0;
        List<ConversationSlaBreachDO> created = new ArrayList<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (ConversationWorkItemDO row : rows) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (!scopedTenantId.equals(row.getTenantId())
                    || row.getSlaDueAt() == null
                    || row.getSlaDueAt().isAfter(effectiveNow)
                    || "RESOLVED".equals(normalizeOptionalUpper(row.getStatus()))) {
                continue;
            }
            scanned++;
            ConversationSlaBreachDO existing = breachMapper.selectOne(new LambdaQueryWrapper<ConversationSlaBreachDO>()
                    .eq(ConversationSlaBreachDO::getTenantId, scopedTenantId)
                    .eq(ConversationSlaBreachDO::getWorkItemId, row.getId())
                    .eq(ConversationSlaBreachDO::getBreachType, "FIRST_RESPONSE")
                    .eq(ConversationSlaBreachDO::getStatus, "OPEN")
                    .last("LIMIT 1"));
            if (existing != null) {
                skippedExisting++;
                continue;
            }
            ConversationSlaBreachDO breach = new ConversationSlaBreachDO();
            breach.setTenantId(scopedTenantId);
            breach.setWorkItemId(row.getId());
            breach.setBreachType("FIRST_RESPONSE");
            breach.setSeverity(slaSeverity(row));
            breach.setStatus("OPEN");
            breach.setEscalationTarget(defaultString(row.getAssignedTeam(), row.getAssignedTo()));
            breach.setReason("SLA due time breached");
            breach.setDueAt(row.getSlaDueAt());
            breach.setBreachedAt(effectiveNow);
            breach.setMetadataJson(writeJson(values(
                    "priority", row.getPriority(),
                    "slaPolicyKey", row.getSlaPolicyKey(),
                    "assignedTo", row.getAssignedTo(),
                    "assignedTeam", row.getAssignedTeam())));
            breach.setCreatedAt(effectiveNow);
            breach.setUpdatedAt(effectiveNow);
            breachMapper.insert(breach);
            created.add(breach);
            if (priorityRank(row.getPriority()) < priorityRank("HIGH")) {
                ConversationWorkItemDO update = new ConversationWorkItemDO();
                update.setId(row.getId());
                update.setPriority("HIGH");
                update.setUpdatedAt(effectiveNow);
                workItemMapper.updateById(update);
            }
            audit(scopedTenantId, row.getId(), "SLA_BREACHED", actor,
                    values("priority", row.getPriority(), "slaDueAt", row.getSlaDueAt()),
                    values("priority", priorityRank(row.getPriority()) < priorityRank("HIGH") ? "HIGH" : row.getPriority(),
                            "breachId", breach.getId()),
                    breach.getReason());
        }
        return new ConversationSlaEvaluationView(
                scopedTenantId,
                scanned,
                created.size(),
                skippedExisting,
                created.stream().map(this::toBreachView).toList());
    }

    /**
     * 查询租户内 SLA 违约记录。
     * 可按状态过滤，按违约时间倒序返回，用于客服主管查看待处理升级项。
     */
    public List<ConversationSlaBreachView> slaBreaches(Long tenantId, String status, int limit) {
        Long scopedTenantId = tenantId(tenantId);
        String normalizedStatus = normalizeOptionalUpper(status);
        int boundedLimit = boundedLimit(limit);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(breachMapper.selectList(new LambdaQueryWrapper<ConversationSlaBreachDO>()
                        .eq(ConversationSlaBreachDO::getTenantId, scopedTenantId)
                        .eq(normalizedStatus != null, ConversationSlaBreachDO::getStatus, normalizedStatus)
                        .orderByDesc(ConversationSlaBreachDO::getBreachedAt)
                        // 遍历候选数据并按业务规则筛选、转换或聚合。
                        .last("LIMIT " + boundedLimit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> normalizedStatus == null || normalizedStatus.equals(row.getStatus()))
                .limit(boundedLimit)
                .map(this::toBreachView)
                .toList();
    }

    /**
     * 执行 routeMiss 流程，围绕 route miss 完成校验、计算或结果组装。
     *
     * @param workItem work item 参数，用于 routeMiss 流程中的校验、计算或对象转换。
     * @param requiredSkills required skills 参数，用于 routeMiss 流程中的校验、计算或对象转换。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 routeMiss 流程生成的业务结果。
     */
    private ConversationRouteResultView routeMiss(ConversationWorkItemDO workItem,
                                                  List<String> requiredSkills,
                                                  String reason,
                                                  String actor) {
        // 准备本次处理所需的上下文和中间变量。
        LocalDateTime changedAt = now();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ConversationWorkItemDO update = new ConversationWorkItemDO();
        update.setId(workItem.getId());
        update.setRoutingStatus("UNROUTED");
        update.setRequiredSkillsJson(writeJson(requiredSkills));
        update.setRoutingReason(reason);
        update.setRoutedAt(changedAt);
        update.setUpdatedAt(changedAt);
        workItemMapper.updateById(update);
        audit(workItem.getTenantId(), workItem.getId(), "ROUTING_MISSED", actor,
                Map.of(),
                values("routingStatus", "UNROUTED", "requiredSkills", requiredSkills, "reason", reason),
                reason);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ConversationRouteResultView(
                workItem.getTenantId(),
                workItem.getId(),
                false,
                null,
                null,
                "UNROUTED",
                reason,
                requiredSkills,
                workItem.getSlaDueAt());
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workItem work item 参数，用于 matchingRule 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private ConversationRoutingRuleDO matchingRule(Long tenantId, ConversationWorkItemDO workItem) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(ruleMapper.selectList(new LambdaQueryWrapper<ConversationRoutingRuleDO>()
                        .eq(ConversationRoutingRuleDO::getTenantId, tenantId)
                        .eq(ConversationRoutingRuleDO::getEnabled, 1)
                        .orderByAsc(ConversationRoutingRuleDO::getSortOrder)
                        // 遍历候选数据并按业务规则筛选、转换或聚合。
                        .orderByAsc(ConversationRoutingRuleDO::getId))).stream()
                .filter(rule -> tenantId.equals(rule.getTenantId()))
                .filter(rule -> enabled(rule.getEnabled()))
                .filter(rule -> rule.getChannel() == null || rule.getChannel().equals(normalizeOptionalUpper(workItem.getChannel())))
                .filter(rule -> priorityRank(workItem.getPriority()) >= priorityRank(rule.getMinPriority()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 执行 bestAgent 流程，围绕 best agent 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param targetTeam target team 参数，用于 bestAgent 流程中的校验、计算或对象转换。
     * @param requiredSkills required skills 参数，用于 bestAgent 流程中的校验、计算或对象转换。
     * @return 返回 bestAgent 流程生成的业务结果。
     */
    private ConversationRoutingAgentDO bestAgent(Long tenantId, String targetTeam, List<String> requiredSkills) {
        Set<String> required = new LinkedHashSet<>(requiredSkills == null ? List.of() : requiredSkills);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(agentMapper.selectList(new LambdaQueryWrapper<ConversationRoutingAgentDO>()
                        .eq(ConversationRoutingAgentDO::getTenantId, tenantId)
                        .eq(ConversationRoutingAgentDO::getStatus, "AVAILABLE")
                        .orderByAsc(ConversationRoutingAgentDO::getCurrentLoad)
                        // 遍历候选数据并按业务规则筛选、转换或聚合。
                        .orderByAsc(ConversationRoutingAgentDO::getAgentKey))).stream()
                .filter(agent -> tenantId.equals(agent.getTenantId()))
                .filter(agent -> "AVAILABLE".equals(normalizeOptionalUpper(agent.getStatus())))
                .filter(agent -> targetTeam == null || targetTeam.equals(agent.getTeamKey()))
                .filter(agent -> value(agent.getCurrentLoad()) < positive(agent.getMaxCapacity(), 1))
                .filter(agent -> new LinkedHashSet<>(stringList(agent.getSkillsJson())).containsAll(required))
                .min(Comparator
                        .comparingInt((ConversationRoutingAgentDO agent) -> value(agent.getCurrentLoad()))
                        .thenComparing(ConversationRoutingAgentDO::getAgentKey))
                .orElse(null);
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workItemId 业务对象 ID，用于定位具体记录。
     * @return 返回 requireWorkItem 流程生成的业务结果。
     */
    private ConversationWorkItemDO requireWorkItem(Long tenantId, Long workItemId) {
        if (workItemId == null) {
            throw new IllegalArgumentException("workItemId is required");
        }
        ConversationWorkItemDO row = workItemMapper.selectById(workItemId);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("Conversation work item not found: " + workItemId);
        }
        return row;
    }

    /**
     * 记录审计、指标或状态变更信息。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workItemId 业务对象 ID，用于定位具体记录。
     * @param eventType 类型标识，用于选择对应处理分支。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param oldValue 待处理值，用于规则计算或转换。
     * @param newValue 待处理值，用于规则计算或转换。
     * @param note note 参数，用于 audit 流程中的校验、计算或对象转换。
     */
    private void audit(Long tenantId,
                       Long workItemId,
                       String eventType,
                       String actor,
                       Map<String, Object> oldValue,
                       Map<String, Object> newValue,
                       String note) {
        ConversationWorkItemAuditDO row = new ConversationWorkItemAuditDO();
        row.setTenantId(tenantId);
        row.setWorkItemId(workItemId);
        row.setEventType(eventType);
        row.setActor(actor(actor));
        row.setOldValueJson(writeJson(oldValue));
        row.setNewValueJson(writeJson(newValue));
        row.setNote(blankToNull(note));
        row.setCreatedAt(now());
        auditMapper.insert(row);
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private ConversationRoutingAgentView toAgentView(ConversationRoutingAgentDO row) {
        return new ConversationRoutingAgentView(
                row.getId(),
                row.getTenantId(),
                row.getAgentKey(),
                row.getDisplayName(),
                row.getTeamKey(),
                row.getStatus(),
                positive(row.getMaxCapacity(), 1),
                value(row.getCurrentLoad()),
                stringList(row.getSkillsJson()),
                map(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private ConversationRoutingRuleView toRuleView(ConversationRoutingRuleDO row) {
        return new ConversationRoutingRuleView(
                row.getId(),
                row.getTenantId(),
                row.getRuleKey(),
                row.getChannel(),
                row.getMinPriority(),
                stringList(row.getRequiredSkillsJson()),
                row.getTargetTeam(),
                positive(row.getSlaMinutes(), 60),
                enabled(row.getEnabled()),
                row.getSortOrder() == null ? 1000 : row.getSortOrder(),
                map(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private ConversationSlaBreachView toBreachView(ConversationSlaBreachDO row) {
        return new ConversationSlaBreachView(
                row.getId(),
                row.getTenantId(),
                row.getWorkItemId(),
                row.getBreachType(),
                row.getSeverity(),
                row.getStatus(),
                row.getEscalationTarget(),
                row.getReason(),
                row.getDueAt(),
                row.getBreachedAt(),
                row.getResolvedBy(),
                row.getResolvedAt(),
                map(row.getMetadataJson()),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 执行 slaSeverity 流程，围绕 sla severity 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 sla severity 生成的文本或业务键。
     */
    private String slaSeverity(ConversationWorkItemDO row) {
        return "URGENT".equals(normalizeOptionalUpper(row.getPriority())) ? "CRITICAL" : "HIGH";
    }

    /**
     * 规范化输入值。
     *
     * @param values values 参数，用于 normalizedKeys 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private List<String> normalizedKeys(List<String> values) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (values == null) {
            return List.of();
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    /**
     * 执行 values 流程，围绕 values 完成校验、计算或结果组装。
     *
     * @param keysAndValues keys and values 参数，用于 values 流程中的校验、计算或对象转换。
     * @return 返回 values 流程生成的业务结果。
     */
    private Map<String, Object> values(Object... keysAndValues) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keysAndValues.length; i += 2) {
            Object key = keysAndValues[i];
            Object value = keysAndValues[i + 1];
            if (key != null) {
                result.put(String.valueOf(key), jsonValue(value));
            }
        }
        return result;
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 jsonValue 流程生成的业务结果。
     */
    private Object jsonValue(Object value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value instanceof LocalDateTime time) {
            return time.toString();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    result.put(String.valueOf(entry.getKey()), jsonValue(entry.getValue()));
                }
            }
            return result;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::jsonValue).toList();
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return value;
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 write json 生成的文本或业务键。
     */
    private String writeJson(Object value) {
        try {
            if (value instanceof List<?> list) {
                return objectMapper.writeValueAsString(new ArrayList<>(list));
            }
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ex) {
            throw new IllegalArgumentException("conversation routing JSON serialization failed", ex);
        }
    }

    /**
     * 执行 stringList 流程，围绕 string list 完成校验、计算或结果组装。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回 string list 汇总后的集合、分页或映射视图。
     */
    private List<String> stringList(String json) {
        if (isBlank(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ignored) {
            return List.of();
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回组装或转换后的结果对象。
     */
    private Map<String, Object> map(String json) {
        if (isBlank(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    /**
     * 执行 priorityRank 流程，围绕 priority rank 完成校验、计算或结果组装。
     *
     * @param priority priority 参数，用于 priorityRank 流程中的校验、计算或对象转换。
     * @return 返回 priority rank 计算得到的数量、金额或指标值。
     */
    private int priorityRank(String priority) {
        return switch (normalizePriority(defaultString(priority, "NORMAL"))) {
            case "URGENT" -> 4;
            case "HIGH" -> 3;
            case "NORMAL" -> 2;
            default -> 1;
        };
    }

    /**
     * 规范化输入值。
     *
     * @param priority priority 参数，用于 normalizePriority 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizePriority(String priority) {
        String normalized = required(priority, "priority is required").toUpperCase(Locale.ROOT);
        if (!PRIORITIES.contains(normalized)) {
            return "NORMAL";
        }
        return normalized;
    }

    /**
     * 规范化输入值。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeAgentStatus(String status) {
        String normalized = defaultString(status, "AVAILABLE").toUpperCase(Locale.ROOT);
        if (!AGENT_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("unsupported routing agent status: " + status);
        }
        return normalized;
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOptionalUpper(String value) {
        return isBlank(value) ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 执行 optionalUpper 流程，围绕 optional upper 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 optional upper 生成的文本或业务键。
     */
    private String optionalUpper(String value) {
        return isBlank(value) ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 执行 optionalKey 流程，围绕 optional key 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 optional key 生成的文本或业务键。
     */
    private String optionalKey(String value) {
        return isBlank(value) ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 执行 key 流程，围绕 key 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 key 生成的文本或业务键。
     */
    private String key(String value, String field) {
        return required(value, field + " is required").toLowerCase(Locale.ROOT);
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    /**
     * 执行 positive 流程，围绕 positive 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 positive 流程中的校验、计算或对象转换。
     * @return 返回 positive 计算得到的数量、金额或指标值。
     */
    private int positive(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    /**
     * 执行 value 流程，围绕 value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 value 计算得到的数量、金额或指标值。
     */
    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 enabled 的布尔判断结果。
     */
    private boolean enabled(Integer value) {
        return value != null && value == 1;
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

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private Long tenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 解析操作人标识。
     *
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 actor 生成的文本或业务键。
     */
    private String actor(String actor) {
        return isBlank(actor) ? "system" : actor.trim();
    }

    /**
     * 执行 now 流程，围绕 now 完成校验、计算或结果组装。
     *
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
