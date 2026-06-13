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

public final class ConversationPersistenceConverter {

    private ConversationPersistenceConverter() {
    }

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

    public static ConversationSession toSession(ConversationSessionDO row) {
        return new ConversationSession(row.id, row.tenantId, row.canvasId, row.versionId,
                row.executionId, row.userId, row.channel, row.provider, row.status,
                value(row.turnCount), parseMap(row.contextJson), row.lastMessageAt, row.expiresAt,
                row.createdAt, row.updatedAt);
    }

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

    public static ConversationMessage toMessage(ConversationMessageDO row) {
        return new ConversationMessage(row.id, row.tenantId, row.sessionId, row.direction,
                row.messageType, row.externalMessageId, row.idempotencyKey, parseMap(row.contentJson),
                row.textContent, row.intent, Boolean.TRUE.equals(row.processed), row.createdAt);
    }

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

    public static ConversationWorkItem toWorkItem(ConversationWorkItemDO row) {
        return new ConversationWorkItem(row.id, row.tenantId, row.sessionId, row.contactProfileId,
                row.userId, row.channel, row.provider, row.subject, row.status, row.priority,
                row.assignedTo, row.assignedTeam, row.source, row.slaDueAt, row.nextFollowUpAt,
                row.lastCustomerMessageAt, row.lastOperatorActivityAt, parseList(row.tagsJson),
                parseMap(row.attributesJson), row.routingStatus, parseList(row.requiredSkillsJson),
                row.routingReason, row.routedAt, row.slaPolicyKey, row.createdAt, row.updatedAt);
    }

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

    public static ConversationContactProfile toContactProfile(ConversationContactProfileDO row) {
        return new ConversationContactProfile(row.id, row.tenantId, row.userId, row.displayName,
                row.externalContactId, row.privateDomainSource, row.owner, row.lifecycleStage,
                parseList(row.tagsJson), parseMap(row.attributesJson), row.createdAt, row.updatedAt);
    }

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

    public static ConversationWorkItemAudit toWorkItemAudit(ConversationWorkItemAuditDO row) {
        return new ConversationWorkItemAudit(row.id, row.tenantId, row.workItemId, row.eventType,
                row.actor, parseMap(row.oldValueJson), parseMap(row.newValueJson), row.note, row.createdAt);
    }

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

    public static ConversationRoutingAgent toRoutingAgent(ConversationRoutingAgentDO row) {
        return new ConversationRoutingAgent(row.id, row.tenantId, row.agentKey, row.displayName,
                row.teamKey, row.status, value(row.maxCapacity, 1), value(row.currentLoad),
                parseList(row.skillsJson), parseMap(row.metadataJson), row.createdBy, row.createdAt, row.updatedAt);
    }

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

    public static ConversationRoutingRule toRoutingRule(ConversationRoutingRuleDO row) {
        return new ConversationRoutingRule(row.id, row.tenantId, row.ruleKey, row.channel,
                row.minPriority, parseList(row.requiredSkillsJson), row.targetTeam,
                value(row.slaMinutes, 60), value(row.enabled, 1) != 0, value(row.sortOrder, 1000),
                parseMap(row.metadataJson), row.createdBy, row.createdAt, row.updatedAt);
    }

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

    public static ConversationSlaBreach toSlaBreach(ConversationSlaBreachDO row) {
        return new ConversationSlaBreach(row.id, row.tenantId, row.workItemId, row.breachType,
                row.severity, row.status, row.escalationTarget, row.reason, row.dueAt, row.breachedAt,
                row.resolvedBy, row.resolvedAt, parseMap(row.metadataJson), row.createdAt, row.updatedAt);
    }

    private static int value(Integer value) {
        return value == null ? 0 : value;
    }

    private static int value(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static String toJson(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return "{}";
        }
        List<String> parts = new ArrayList<>();
        values.forEach((key, value) -> parts.add(quote(key) + ":" + toJsonValue(value)));
        return "{" + String.join(",", parts) + "}";
    }

    private static String toJson(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        return "[" + String.join(",", values.stream().map(ConversationPersistenceConverter::quote).toList()) + "]";
    }

    private static String toJsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new LinkedHashMap<>();
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

    private static String trimBrackets(String value, char open, char close) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.charAt(0) == open && trimmed.charAt(trimmed.length() - 1) == close) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

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

    private static String quote(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

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
