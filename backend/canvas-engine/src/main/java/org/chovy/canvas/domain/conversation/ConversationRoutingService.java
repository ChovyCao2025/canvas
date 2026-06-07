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
    public ConversationRoutingAgentView upsertAgent(Long tenantId,
                                                    ConversationRoutingAgentCommand command,
                                                    String actor) {
        if (command == null) {
            throw new IllegalArgumentException("conversation routing agent command is required");
        }
        Long scopedTenantId = tenantId(tenantId);
        String agentKey = key(command.agentKey(), "agentKey");
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
        return toAgentView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public ConversationRoutingRuleView upsertRule(Long tenantId,
                                                  ConversationRoutingRuleCommand command,
                                                  String actor) {
        if (command == null) {
            throw new IllegalArgumentException("conversation routing rule command is required");
        }
        Long scopedTenantId = tenantId(tenantId);
        String ruleKey = key(command.ruleKey(), "ruleKey");
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
        return toRuleView(row);
    }

    @Transactional(rollbackFor = Exception.class)
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
                ? normalizedKeys(command.requiredSkills())
                : rule == null ? List.of() : stringList(rule.getRequiredSkillsJson());
        String targetTeam = command == null || isBlank(command.targetTeam())
                ? rule == null ? null : rule.getTargetTeam()
                : optionalKey(command.targetTeam());
        int slaMinutes = command == null || command.slaMinutes() == null
                ? rule == null ? 60 : positive(rule.getSlaMinutes(), 60)
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
    public ConversationSlaEvaluationView evaluateSlaBreaches(Long tenantId,
                                                             LocalDateTime evaluatedAt,
                                                             String actor,
                                                             int limit) {
        Long scopedTenantId = tenantId(tenantId);
        LocalDateTime effectiveNow = evaluatedAt == null ? now() : evaluatedAt;
        int boundedLimit = boundedLimit(limit);
        List<ConversationWorkItemDO> rows = safeList(workItemMapper.selectList(new LambdaQueryWrapper<ConversationWorkItemDO>()
                .eq(ConversationWorkItemDO::getTenantId, scopedTenantId)
                .le(ConversationWorkItemDO::getSlaDueAt, effectiveNow)
                .last("LIMIT " + boundedLimit)));
        int scanned = 0;
        int skippedExisting = 0;
        List<ConversationSlaBreachDO> created = new ArrayList<>();
        for (ConversationWorkItemDO row : rows) {
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

    public List<ConversationSlaBreachView> slaBreaches(Long tenantId, String status, int limit) {
        Long scopedTenantId = tenantId(tenantId);
        String normalizedStatus = normalizeOptionalUpper(status);
        int boundedLimit = boundedLimit(limit);
        return safeList(breachMapper.selectList(new LambdaQueryWrapper<ConversationSlaBreachDO>()
                        .eq(ConversationSlaBreachDO::getTenantId, scopedTenantId)
                        .eq(normalizedStatus != null, ConversationSlaBreachDO::getStatus, normalizedStatus)
                        .orderByDesc(ConversationSlaBreachDO::getBreachedAt)
                        .last("LIMIT " + boundedLimit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> normalizedStatus == null || normalizedStatus.equals(row.getStatus()))
                .limit(boundedLimit)
                .map(this::toBreachView)
                .toList();
    }

    private ConversationRouteResultView routeMiss(ConversationWorkItemDO workItem,
                                                  List<String> requiredSkills,
                                                  String reason,
                                                  String actor) {
        LocalDateTime changedAt = now();
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

    private ConversationRoutingRuleDO matchingRule(Long tenantId, ConversationWorkItemDO workItem) {
        return safeList(ruleMapper.selectList(new LambdaQueryWrapper<ConversationRoutingRuleDO>()
                        .eq(ConversationRoutingRuleDO::getTenantId, tenantId)
                        .eq(ConversationRoutingRuleDO::getEnabled, 1)
                        .orderByAsc(ConversationRoutingRuleDO::getSortOrder)
                        .orderByAsc(ConversationRoutingRuleDO::getId))).stream()
                .filter(rule -> tenantId.equals(rule.getTenantId()))
                .filter(rule -> enabled(rule.getEnabled()))
                .filter(rule -> rule.getChannel() == null || rule.getChannel().equals(normalizeOptionalUpper(workItem.getChannel())))
                .filter(rule -> priorityRank(workItem.getPriority()) >= priorityRank(rule.getMinPriority()))
                .findFirst()
                .orElse(null);
    }

    private ConversationRoutingAgentDO bestAgent(Long tenantId, String targetTeam, List<String> requiredSkills) {
        Set<String> required = new LinkedHashSet<>(requiredSkills == null ? List.of() : requiredSkills);
        return safeList(agentMapper.selectList(new LambdaQueryWrapper<ConversationRoutingAgentDO>()
                        .eq(ConversationRoutingAgentDO::getTenantId, tenantId)
                        .eq(ConversationRoutingAgentDO::getStatus, "AVAILABLE")
                        .orderByAsc(ConversationRoutingAgentDO::getCurrentLoad)
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

    private String slaSeverity(ConversationWorkItemDO row) {
        return "URGENT".equals(normalizeOptionalUpper(row.getPriority())) ? "CRITICAL" : "HIGH";
    }

    private List<String> normalizedKeys(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

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

    private Object jsonValue(Object value) {
        if (value instanceof LocalDateTime time) {
            return time.toString();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
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
        return value;
    }

    private String writeJson(Object value) {
        try {
            if (value instanceof List<?> list) {
                return objectMapper.writeValueAsString(new ArrayList<>(list));
            }
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("conversation routing JSON serialization failed", ex);
        }
    }

    private List<String> stringList(String json) {
        if (isBlank(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private Map<String, Object> map(String json) {
        if (isBlank(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    private int priorityRank(String priority) {
        return switch (normalizePriority(defaultString(priority, "NORMAL"))) {
            case "URGENT" -> 4;
            case "HIGH" -> 3;
            case "NORMAL" -> 2;
            default -> 1;
        };
    }

    private String normalizePriority(String priority) {
        String normalized = required(priority, "priority is required").toUpperCase(Locale.ROOT);
        if (!PRIORITIES.contains(normalized)) {
            return "NORMAL";
        }
        return normalized;
    }

    private String normalizeAgentStatus(String status) {
        String normalized = defaultString(status, "AVAILABLE").toUpperCase(Locale.ROOT);
        if (!AGENT_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("unsupported routing agent status: " + status);
        }
        return normalized;
    }

    private String normalizeOptionalUpper(String value) {
        return isBlank(value) ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String optionalUpper(String value) {
        return isBlank(value) ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String optionalKey(String value) {
        return isBlank(value) ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String key(String value, String field) {
        return required(value, field + " is required").toLowerCase(Locale.ROOT);
    }

    private String required(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String defaultString(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private int positive(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean enabled(Integer value) {
        return value != null && value == 1;
    }

    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    private Long tenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String actor(String actor) {
        return isBlank(actor) ? "system" : actor.trim();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
