package org.chovy.canvas.conversation.adapter.persistence;

import org.chovy.canvas.conversation.domain.ConversationMessage;
import org.chovy.canvas.conversation.domain.ConversationContactProfile;
import org.chovy.canvas.conversation.domain.ConversationRoutingAgent;
import org.chovy.canvas.conversation.domain.ConversationRoutingRule;
import org.chovy.canvas.conversation.domain.ConversationSession;
import org.chovy.canvas.conversation.domain.ConversationSlaBreach;
import org.chovy.canvas.conversation.domain.ConversationWorkItem;
import org.chovy.canvas.conversation.domain.ConversationWorkItemAudit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话领域对象和持久化行对象之间的转换器。
 */
public final class ConversationPersistenceConverter {

    /**
     * 禁止实例化纯静态转换器。
     */
    private ConversationPersistenceConverter() {
    }

    /**
     * 将会话领域对象转换为数据库行对象。
     *
     * @param session 会话领域对象
     * @return 会话数据库行
     */
    public static ConversationSessionDO toSessionRow(ConversationSession session) {
        ConversationSessionDO row = new ConversationSessionDO();
        row.id = session.id();
        row.tenantId = session.tenantId();
        row.canvasId = session.canvasId();
        row.versionId = session.versionId();
        row.executionId = session.executionId();
        row.userId = session.userId();
        row.channel = session.channel();
        row.provider = session.provider();
        row.status = session.status();
        row.turnCount = session.turnCount();
        row.contextJson = toJson(session.context());
        row.lastMessageAt = session.lastMessageAt();
        row.expiresAt = session.expiresAt();
        row.createdAt = session.createdAt();
        row.updatedAt = session.updatedAt();
        return row;
    }

    /**
     * 将会话数据库行转换为领域对象。
     *
     * @param row 会话数据库行
     * @return 会话领域对象
     */
    public static ConversationSession toSession(ConversationSessionDO row) {
        return new ConversationSession(row.id, row.tenantId, row.canvasId, row.versionId,
                row.executionId, row.userId, row.channel, row.provider, row.status,
                value(row.turnCount), parseMap(row.contextJson), row.lastMessageAt, row.expiresAt,
                row.createdAt, row.updatedAt);
    }

    /**
     * 将消息领域对象转换为数据库行对象。
     *
     * @param message 消息领域对象
     * @return 消息数据库行
     */
    public static ConversationMessageDO toMessageRow(ConversationMessage message) {
        ConversationMessageDO row = new ConversationMessageDO();
        row.id = message.id();
        row.tenantId = message.tenantId();
        row.sessionId = message.sessionId();
        row.direction = message.direction();
        row.messageType = message.messageType();
        row.externalMessageId = message.externalMessageId();
        row.idempotencyKey = message.idempotencyKey();
        row.contentJson = toJson(message.content());
        row.textContent = message.textContent();
        row.intent = message.intent();
        row.processed = message.processed();
        row.createdAt = message.createdAt();
        return row;
    }

    /**
     * 将消息数据库行转换为领域对象。
     *
     * @param row 消息数据库行
     * @return 消息领域对象
     */
    public static ConversationMessage toMessage(ConversationMessageDO row) {
        return new ConversationMessage(row.id, row.tenantId, row.sessionId, row.direction,
                row.messageType, row.externalMessageId, row.idempotencyKey, parseMap(row.contentJson),
                row.textContent, row.intent, Boolean.TRUE.equals(row.processed), row.createdAt);
    }

    /**
     * 将工单领域对象转换为数据库行对象。
     *
     * @param item 工单领域对象
     * @return 工单数据库行
     */
    public static ConversationWorkItemDO toWorkItemRow(ConversationWorkItem item) {
        ConversationWorkItemDO row = new ConversationWorkItemDO();
        row.id = item.id();
        row.tenantId = item.tenantId();
        row.sessionId = item.sessionId();
        row.contactProfileId = item.contactProfileId();
        row.userId = item.userId();
        row.channel = item.channel();
        row.provider = item.provider();
        row.subject = item.subject();
        row.status = item.status();
        row.priority = item.priority();
        row.assignedTo = item.assignedTo();
        row.assignedTeam = item.assignedTeam();
        row.source = item.source();
        row.slaDueAt = item.slaDueAt();
        row.nextFollowUpAt = item.nextFollowUpAt();
        row.lastCustomerMessageAt = item.lastCustomerMessageAt();
        row.lastOperatorActivityAt = item.lastOperatorActivityAt();
        row.tagsJson = toJson(item.tags());
        row.attributesJson = toJson(item.attributes());
        row.routingStatus = item.routingStatus();
        row.requiredSkillsJson = toJson(item.requiredSkills());
        row.routingReason = item.routingReason();
        row.routedAt = item.routedAt();
        row.slaPolicyKey = item.slaPolicyKey();
        row.createdAt = item.createdAt();
        row.updatedAt = item.updatedAt();
        return row;
    }

    /**
     * 将工单数据库行转换为领域对象。
     *
     * @param row 工单数据库行
     * @return 工单领域对象
     */
    public static ConversationWorkItem toWorkItem(ConversationWorkItemDO row) {
        return new ConversationWorkItem(row.id, row.tenantId, row.sessionId, row.contactProfileId,
                row.userId, row.channel, row.provider, row.subject, row.status, row.priority,
                row.assignedTo, row.assignedTeam, row.source, row.slaDueAt, row.nextFollowUpAt,
                row.lastCustomerMessageAt, row.lastOperatorActivityAt, parseList(row.tagsJson),
                parseMap(row.attributesJson), row.routingStatus, parseList(row.requiredSkillsJson),
                row.routingReason, row.routedAt, row.slaPolicyKey, row.createdAt, row.updatedAt);
    }

    /**
     * 将联系人画像领域对象转换为数据库行对象。
     *
     * @param profile 联系人画像领域对象
     * @return 联系人画像数据库行
     */
    public static ConversationContactProfileDO toContactProfileRow(ConversationContactProfile profile) {
        ConversationContactProfileDO row = new ConversationContactProfileDO();
        row.id = profile.id();
        row.tenantId = profile.tenantId();
        row.userId = profile.userId();
        row.displayName = profile.displayName();
        row.externalContactId = profile.externalContactId();
        row.privateDomainSource = profile.privateDomainSource();
        row.owner = profile.owner();
        row.lifecycleStage = profile.lifecycleStage();
        row.tagsJson = toJson(profile.tags());
        row.attributesJson = toJson(profile.attributes());
        row.createdAt = profile.createdAt();
        row.updatedAt = profile.updatedAt();
        return row;
    }

    /**
     * 将联系人画像数据库行转换为领域对象。
     *
     * @param row 联系人画像数据库行
     * @return 联系人画像领域对象
     */
    public static ConversationContactProfile toContactProfile(ConversationContactProfileDO row) {
        return new ConversationContactProfile(row.id, row.tenantId, row.userId, row.displayName,
                row.externalContactId, row.privateDomainSource, row.owner, row.lifecycleStage,
                parseList(row.tagsJson), parseMap(row.attributesJson), row.createdAt, row.updatedAt);
    }

    /**
     * 将工单审计领域对象转换为数据库行对象。
     *
     * @param audit 工单审计领域对象
     * @return 工单审计数据库行
     */
    public static ConversationWorkItemAuditDO toWorkItemAuditRow(ConversationWorkItemAudit audit) {
        ConversationWorkItemAuditDO row = new ConversationWorkItemAuditDO();
        row.id = audit.id();
        row.tenantId = audit.tenantId();
        row.workItemId = audit.workItemId();
        row.eventType = audit.eventType();
        row.actor = audit.actor();
        row.oldValueJson = toJson(audit.oldValue());
        row.newValueJson = toJson(audit.newValue());
        row.note = audit.note();
        row.createdAt = audit.createdAt();
        return row;
    }

    /**
     * 将工单审计数据库行转换为领域对象。
     *
     * @param row 工单审计数据库行
     * @return 工单审计领域对象
     */
    public static ConversationWorkItemAudit toWorkItemAudit(ConversationWorkItemAuditDO row) {
        return new ConversationWorkItemAudit(row.id, row.tenantId, row.workItemId, row.eventType,
                row.actor, parseMap(row.oldValueJson), parseMap(row.newValueJson), row.note, row.createdAt);
    }

    /**
     * 将路由坐席领域对象转换为数据库行对象。
     *
     * @param agent 路由坐席领域对象
     * @return 路由坐席数据库行
     */
    public static ConversationRoutingAgentDO toRoutingAgentRow(ConversationRoutingAgent agent) {
        ConversationRoutingAgentDO row = new ConversationRoutingAgentDO();
        row.id = agent.id();
        row.tenantId = agent.tenantId();
        row.agentKey = agent.agentKey();
        row.displayName = agent.displayName();
        row.teamKey = agent.teamKey();
        row.status = agent.status();
        row.maxCapacity = agent.maxCapacity();
        row.currentLoad = agent.currentLoad();
        row.skillsJson = toJson(agent.skills());
        row.metadataJson = toJson(agent.metadata());
        row.createdBy = agent.createdBy();
        row.createdAt = agent.createdAt();
        row.updatedAt = agent.updatedAt();
        return row;
    }

    /**
     * 将路由坐席数据库行转换为领域对象。
     *
     * @param row 路由坐席数据库行
     * @return 路由坐席领域对象
     */
    public static ConversationRoutingAgent toRoutingAgent(ConversationRoutingAgentDO row) {
        return new ConversationRoutingAgent(row.id, row.tenantId, row.agentKey, row.displayName,
                row.teamKey, row.status, value(row.maxCapacity, 1), value(row.currentLoad),
                parseList(row.skillsJson), parseMap(row.metadataJson), row.createdBy, row.createdAt, row.updatedAt);
    }

    /**
     * 将路由规则领域对象转换为数据库行对象。
     *
     * @param rule 路由规则领域对象
     * @return 路由规则数据库行
     */
    public static ConversationRoutingRuleDO toRoutingRuleRow(ConversationRoutingRule rule) {
        ConversationRoutingRuleDO row = new ConversationRoutingRuleDO();
        row.id = rule.id();
        row.tenantId = rule.tenantId();
        row.ruleKey = rule.ruleKey();
        row.channel = rule.channel();
        row.minPriority = rule.minPriority();
        row.requiredSkillsJson = toJson(rule.requiredSkills());
        row.targetTeam = rule.targetTeam();
        row.slaMinutes = rule.slaMinutes();
        row.enabled = rule.enabled() ? 1 : 0;
        row.sortOrder = rule.sortOrder();
        row.metadataJson = toJson(rule.metadata());
        row.createdBy = rule.createdBy();
        row.createdAt = rule.createdAt();
        row.updatedAt = rule.updatedAt();
        return row;
    }

    /**
     * 将路由规则数据库行转换为领域对象。
     *
     * @param row 路由规则数据库行
     * @return 路由规则领域对象
     */
    public static ConversationRoutingRule toRoutingRule(ConversationRoutingRuleDO row) {
        return new ConversationRoutingRule(row.id, row.tenantId, row.ruleKey, row.channel,
                row.minPriority, parseList(row.requiredSkillsJson), row.targetTeam,
                value(row.slaMinutes, 60), value(row.enabled, 1) != 0, value(row.sortOrder, 1000),
                parseMap(row.metadataJson), row.createdBy, row.createdAt, row.updatedAt);
    }

    /**
     * 将 SLA 违约领域对象转换为数据库行对象。
     *
     * @param breach SLA 违约领域对象
     * @return SLA 违约数据库行
     */
    public static ConversationSlaBreachDO toSlaBreachRow(ConversationSlaBreach breach) {
        ConversationSlaBreachDO row = new ConversationSlaBreachDO();
        row.id = breach.id();
        row.tenantId = breach.tenantId();
        row.workItemId = breach.workItemId();
        row.breachType = breach.breachType();
        row.severity = breach.severity();
        row.status = breach.status();
        row.escalationTarget = breach.escalationTarget();
        row.reason = breach.reason();
        row.dueAt = breach.dueAt();
        row.breachedAt = breach.breachedAt();
        row.resolvedBy = breach.resolvedBy();
        row.resolvedAt = breach.resolvedAt();
        row.metadataJson = toJson(breach.metadata());
        row.createdAt = breach.createdAt();
        row.updatedAt = breach.updatedAt();
        return row;
    }

    /**
     * 将 SLA 违约数据库行转换为领域对象。
     *
     * @param row SLA 违约数据库行
     * @return SLA 违约领域对象
     */
    public static ConversationSlaBreach toSlaBreach(ConversationSlaBreachDO row) {
        return new ConversationSlaBreach(row.id, row.tenantId, row.workItemId, row.breachType,
                row.severity, row.status, row.escalationTarget, row.reason, row.dueAt, row.breachedAt,
                row.resolvedBy, row.resolvedAt, parseMap(row.metadataJson), row.createdAt, row.updatedAt);
    }

    /**
     * 将可空整数转换为 0。
     *
     * @param value 可空整数
     * @return 非空整数
     */
    private static int value(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 将可空整数转换为给定默认值。
     *
     * @param value 可空整数
     * @param defaultValue 默认值
     * @return 非空整数
     */
    private static int value(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * 将 Map 序列化为当前模块使用的轻量 JSON 文本。
     *
     * @param values 待序列化 Map
     * @return JSON 对象文本
     */
    private static String toJson(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return "{}";
        }
        List<String> parts = new ArrayList<>();
        values.forEach((key, value) -> parts.add(quote(key) + ":" + toJsonValue(value)));
        return "{" + String.join(",", parts) + "}";
    }

    /**
     * 将字符串列表序列化为 JSON 数组文本。
     *
     * @param values 待序列化列表
     * @return JSON 数组文本
     */
    private static String toJson(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        return "[" + String.join(",", values.stream().map(ConversationPersistenceConverter::quote).toList()) + "]";
    }

    /**
     * 将单个值序列化为 JSON 值文本。
     *
     * @param value 待序列化值
     * @return JSON 值文本
     */
    private static String toJsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            // JSON 对象键统一转成字符串，保持写库格式稳定。
            map.forEach((key, nestedValue) -> {
                if (key != null) {
                    normalized.put(String.valueOf(key), nestedValue);
                }
            });
            return toJson(normalized);
        }
        if (value instanceof List<?> list) {
            return "[" + String.join(",", list.stream().map(ConversationPersistenceConverter::toJsonValue).toList()) + "]";
        }
        return quote(String.valueOf(value));
    }

    /**
     * 将轻量 JSON 对象文本解析为 Map。
     *
     * @param json JSON 对象文本
     * @return 解析后的 Map
     */
    private static Map<String, Object> parseMap(String json) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (json == null || json.isBlank() || "{}".equals(json.trim())) {
            return values;
        }
        String body = trimBrackets(json.trim(), '{', '}');
        for (String part : splitTopLevel(body)) {
            int separator = part.indexOf(':');
            if (separator <= 0) {
                continue;
            }
            String key = unquote(part.substring(0, separator).trim());
            String rawValue = part.substring(separator + 1).trim();
            values.put(key, parseValue(rawValue));
        }
        return values;
    }

    /**
     * 将轻量 JSON 数组文本解析为字符串列表。
     *
     * @param json JSON 数组文本
     * @return 解析后的字符串列表
     */
    private static List<String> parseList(String json) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return List.of();
        }
        String body = trimBrackets(json.trim(), '[', ']');
        return splitTopLevel(body).stream()
                .map(String::trim)
                .map(ConversationPersistenceConverter::unquote)
                .toList();
    }

    /**
     * 解析单个 JSON 值文本。
     *
     * @param rawValue JSON 值文本
     * @return 解析后的 Java 值
     */
    private static Object parseValue(String rawValue) {
        if (rawValue.startsWith("{")) {
            return parseMap(rawValue);
        }
        if (rawValue.startsWith("[")) {
            return parseList(rawValue);
        }
        if ("null".equals(rawValue)) {
            return null;
        }
        if ("true".equals(rawValue) || "false".equals(rawValue)) {
            return Boolean.valueOf(rawValue);
        }
        try {
            return Long.valueOf(rawValue);
        } catch (NumberFormatException ignored) {
            return unquote(rawValue);
        }
    }

    /**
     * 在首尾括号匹配时剥离括号。
     *
     * @param value 原始文本
     * @param open 左括号
     * @param close 右括号
     * @return 去括号后的文本
     */
    private static String trimBrackets(String value, char open, char close) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.charAt(0) == open && trimmed.charAt(trimmed.length() - 1) == close) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    /**
     * 按顶层逗号切分 JSON 对象或数组正文。
     *
     * @param body JSON 对象或数组正文
     * @return 顶层片段列表
     */
    private static List<String> splitTopLevel(String body) {
        List<String> parts = new ArrayList<>();
        if (body.isBlank()) {
            return parts;
        }
        int depth = 0;
        boolean quoted = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < body.length(); i++) {
            char ch = body.charAt(i);
            if (ch == '"' && (i == 0 || body.charAt(i - 1) != '\\')) {
                quoted = !quoted;
            }
            if (!quoted) {
                if (ch == '{' || ch == '[') {
                    depth++;
                } else if (ch == '}' || ch == ']') {
                    depth--;
                } else if (ch == ',' && depth == 0) {
                    parts.add(current.toString());
                    current.setLength(0);
                    continue;
                }
            }
            current.append(ch);
        }
        parts.add(current.toString());
        return parts;
    }

    /**
     * 将文本转义并包裹为 JSON 字符串。
     *
     * @param value 原始文本
     * @return JSON 字符串文本
     */
    private static String quote(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /**
     * 去除 JSON 字符串包裹并还原简单转义。
     *
     * @param value JSON 字符串文本
     * @return 还原后的文本
     */
    private static String unquote(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.charAt(0) == '"' && trimmed.charAt(trimmed.length() - 1) == '"') {
            return trimmed.substring(1, trimmed.length() - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
        return trimmed;
    }
}
