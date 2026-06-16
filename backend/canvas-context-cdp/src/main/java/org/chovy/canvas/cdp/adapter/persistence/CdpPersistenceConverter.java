package org.chovy.canvas.cdp.adapter.persistence;

import org.chovy.canvas.cdp.domain.AudienceSnapshot;
import org.chovy.canvas.cdp.domain.AudienceSnapshotMode;
import org.chovy.canvas.cdp.domain.CdpEventLog;
import org.chovy.canvas.cdp.domain.CdpTagDefinition;
import org.chovy.canvas.cdp.domain.CdpUserTag;
import org.chovy.canvas.cdp.domain.CdpUserTagHistory;
import org.chovy.canvas.cdp.domain.CustomerProfile;
import org.chovy.canvas.cdp.domain.WarehouseIncident;
import org.chovy.canvas.cdp.domain.WarehouseMaterializationRun;
import org.chovy.canvas.cdp.domain.WarehouseSyncRun;
import org.chovy.canvas.cdp.domain.WarehouseWatermark;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 表示 CdpPersistenceConverter 的业务数据或处理组件。
 */
@Component
public class CdpPersistenceConverter {

    /**
     * 转换为Profile Row。
     */
    public CdpUserProfileDO toProfileRow(CustomerProfile profile) {
        if (profile == null) {
            return null;
        }
        CdpUserProfileDO row = new CdpUserProfileDO();
        row.setId(profile.id());
        row.setTenantId(profile.tenantId());
        row.setUserId(profile.userId());
        row.setDisplayName(profile.displayName());
        row.setPhone(profile.phone());
        row.setEmail(profile.email());
        row.setStatus(profile.status());
        row.setPropertiesJson(SimpleJsonCodec.toJsonObject(profile.properties()));
        row.setFirstSeenAt(profile.firstSeenAt());
        row.setLastSeenAt(profile.lastSeenAt());
        row.setCreatedBy(profile.createdBy());
        row.setCreatedAt(profile.createdAt());
        row.setUpdatedAt(profile.updatedAt());
        return row;
    }

    /**
     * 转换为Profile。
     */
    public CustomerProfile toProfile(CdpUserProfileDO row) {
        if (row == null) {
            return null;
        }
        return new CustomerProfile(
                row.getId(),
                row.getTenantId(),
                row.getUserId(),
                row.getDisplayName(),
                row.getPhone(),
                row.getEmail(),
                row.getStatus(),
                SimpleJsonCodec.fromJsonObject(row.getPropertiesJson()),
                row.getFirstSeenAt(),
                row.getLastSeenAt(),
                row.getCreatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 转换为Identity Row。
     */
    public CdpUserIdentityDO toIdentityRow(Long tenantId, String userId, String identityType, String identityValue,
                                           String sourceType, String sourceRefId, boolean verified) {
        CdpUserIdentityDO row = new CdpUserIdentityDO();
        row.setTenantId(tenantId);
        row.setUserId(userId);
        row.setIdentityType(identityType);
        row.setIdentityValue(identityValue);
        row.setSourceType(sourceType);
        row.setSourceRefId(sourceRefId);
        row.setVerified(verified ? 1 : 0);
        return row;
    }

    /**
     * 转换为Tag Definition。
     */
    public CdpTagDefinition toTagDefinition(TagDefinitionDO row) {
        if (row == null) {
            return null;
        }
        return new CdpTagDefinition(
                row.getTagCode(),
                row.getName(),
                row.getValueType(),
                row.getEnabled() == null || row.getEnabled() == 1,
                row.getManualEnabled() == null || row.getManualEnabled() == 1,
                row.getDefaultTtlDays());
    }

    /**
     * 转换为User Tag Row。
     */
    public CdpUserTagDO toUserTagRow(CdpUserTag tag) {
        if (tag == null) {
            return null;
        }
        CdpUserTagDO row = new CdpUserTagDO();
        row.setId(tag.id());
        row.setTenantId(tag.tenantId());
        row.setUserId(tag.userId());
        row.setTagCode(tag.tagCode());
        row.setTagValue(tag.tagValue());
        row.setValueType(tag.valueType());
        row.setSourceType(tag.sourceType());
        row.setSourceRefId(tag.sourceRefId());
        row.setStatus(tag.status());
        row.setEffectiveAt(tag.effectiveAt());
        row.setExpiresAt(tag.expiresAt());
        row.setCreatedBy(tag.createdBy());
        row.setCreatedAt(tag.createdAt());
        row.setUpdatedAt(tag.updatedAt());
        return row;
    }

    /**
     * 转换为User Tag。
     */
    public CdpUserTag toUserTag(CdpUserTagDO row) {
        if (row == null) {
            return null;
        }
        return new CdpUserTag(
                row.getId(),
                row.getTenantId(),
                row.getUserId(),
                row.getTagCode(),
                row.getTagValue(),
                row.getValueType(),
                row.getSourceType(),
                row.getSourceRefId(),
                row.getStatus(),
                row.getEffectiveAt(),
                row.getExpiresAt(),
                row.getCreatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 转换为User Tag History Row。
     */
    public CdpUserTagHistoryDO toUserTagHistoryRow(CdpUserTagHistory history) {
        if (history == null) {
            return null;
        }
        CdpUserTagHistoryDO row = new CdpUserTagHistoryDO();
        row.setTenantId(history.tenantId());
        row.setUserId(history.userId());
        row.setTagCode(history.tagCode());
        row.setOldValue(history.oldValue());
        row.setNewValue(history.newValue());
        row.setOperation(history.operation());
        row.setSourceType(history.sourceType());
        row.setSourceRefId(history.sourceRefId());
        row.setIdempotencyKey(history.idempotencyKey());
        row.setReason(history.reason());
        row.setOperator(history.operator());
        row.setOperatedAt(history.operatedAt());
        return row;
    }

    /**
     * 转换为User Tag History。
     */
    public CdpUserTagHistory toUserTagHistory(CdpUserTagHistoryDO row) {
        if (row == null) {
            return null;
        }
        return new CdpUserTagHistory(
                row.getTenantId(),
                row.getUserId(),
                row.getTagCode(),
                row.getOldValue(),
                row.getNewValue(),
                row.getOperation(),
                row.getSourceType(),
                row.getSourceRefId(),
                row.getIdempotencyKey(),
                row.getReason(),
                row.getOperator(),
                row.getOperatedAt());
    }

    /**
     * 转换为Event Row。
     */
    public CdpEventLogDO toEventRow(CdpEventLog eventLog) {
        if (eventLog == null) {
            return null;
        }
        CdpEventLogDO row = new CdpEventLogDO();
        row.setId(eventLog.id());
        row.setTenantId(eventLog.tenantId());
        row.setWriteKeyId(eventLog.writeKeyId());
        row.setMessageId(eventLog.messageId());
        row.setEventType(eventLog.eventType());
        row.setEventCode(eventLog.eventCode());
        row.setUserId(eventLog.userId());
        row.setAnonymousId(eventLog.anonymousId());
        row.setSessionId(eventLog.sessionId());
        row.setDeviceId(eventLog.deviceId());
        row.setPlatform(eventLog.platform());
        row.setSdkContext(SimpleJsonCodec.toJsonObject(eventLog.sdkContext()));
        row.setProperties(SimpleJsonCodec.toJsonObject(eventLog.properties()));
        row.setIdempotencyKey(eventLog.idempotencyKey());
        row.setEventTime(eventLog.eventTime());
        row.setSentAt(eventLog.sentAt());
        row.setReceivedAt(eventLog.receivedAt());
        row.setStatus(eventLog.status());
        row.setErrorMessage(eventLog.errorMessage());
        row.setCreatedAt(eventLog.createdAt());
        return row;
    }

    /**
     * 转换为Event。
     */
    public CdpEventLog toEvent(CdpEventLogDO row) {
        if (row == null) {
            return null;
        }
        return new CdpEventLog(
                row.getId(),
                row.getTenantId(),
                row.getWriteKeyId(),
                row.getMessageId(),
                row.getEventType(),
                row.getEventCode(),
                row.getUserId(),
                row.getAnonymousId(),
                row.getSessionId(),
                row.getDeviceId(),
                row.getPlatform(),
                SimpleJsonCodec.fromJsonObject(row.getSdkContext()),
                SimpleJsonCodec.fromJsonObject(row.getProperties()),
                row.getIdempotencyKey(),
                row.getEventTime(),
                row.getSentAt(),
                row.getReceivedAt(),
                row.getStatus(),
                row.getErrorMessage(),
                row.getCreatedAt());
    }

    /**
     * 转换为Audience Snapshot Row。
     */
    public AudienceSnapshotDO toAudienceSnapshotRow(AudienceSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        AudienceSnapshotDO row = new AudienceSnapshotDO();
        row.setId(snapshot.id());
        row.setAudienceId(snapshot.audienceId());
        row.setCanvasId(snapshot.canvasId());
        row.setCanvasVersionId(snapshot.canvasVersionId());
        row.setNodeId(snapshot.nodeId());
        row.setSnapshotMode(snapshot.snapshotMode().name());
        row.setUserCount(snapshot.userCount());
        row.setUserIdsJson(SimpleJsonCodec.toJsonArray(snapshot.userIds()));
        row.setCreatedBy(snapshot.createdBy());
        row.setCreatedAt(snapshot.createdAt());
        return row;
    }

    /**
     * 转换为Audience Snapshot。
     */
    public AudienceSnapshot toAudienceSnapshot(AudienceSnapshotDO row) {
        if (row == null) {
            return null;
        }
        return new AudienceSnapshot(
                row.getId(),
                row.getAudienceId(),
                row.getCanvasId(),
                row.getCanvasVersionId(),
                row.getNodeId(),
                AudienceSnapshotMode.normalize(row.getSnapshotMode()),
                SimpleJsonCodec.fromJsonStringArray(row.getUserIdsJson()),
                row.getCreatedBy(),
                row.getCreatedAt());
    }

    /**
     * 转换为Incident。
     */
    public WarehouseIncident toIncident(CdpWarehouseIncidentDO row) {
        return row == null ? null : new WarehouseIncident(row.getSeverity(), row.getStatus());
    }

    /**
     * 转换为Sync Run。
     */
    public WarehouseSyncRun toSyncRun(CdpWarehouseSyncRunDO row) {
        return row == null ? null : new WarehouseSyncRun(
                row.getStatus(),
                row.getFinishedAt(),
                row.getStartedAt(),
                row.getWindowEnd(),
                row.getWindowStart());
    }

    /**
     * 转换为Watermark。
     */
    public WarehouseWatermark toWatermark(CdpWarehouseWatermarkDO row) {
        return row == null ? null : new WarehouseWatermark(row.getWatermarkTime(), row.getUpdatedAt());
    }

    /**
     * 转换为Materialization Run。
     */
    public WarehouseMaterializationRun toMaterializationRun(AudienceMaterializationRunDO row) {
        return row == null ? null : new WarehouseMaterializationRun(
                row.getStatus(),
                row.getFinishedAt(),
                row.getStartedAt());
    }

    /**
     * 表示 SimpleJsonCodec 的业务数据或处理组件。
     */
    private static final class SimpleJsonCodec {

        /**
         * 创建当前组件实例。
         */
        private SimpleJsonCodec() {
        }

        /**
         * 转换为Json Object。
         */
        static String toJsonObject(Map<String, Object> value) {
            return value == null || value.isEmpty() ? "{}" : mapToJson(value);
        }

        /**
         * 转换为Json Array。
         */
        static String toJsonArray(List<String> value) {
            if (value == null || value.isEmpty()) {
                return "[]";
            }
            StringBuilder builder = new StringBuilder("[");
            for (int index = 0; index < value.size(); index++) {
                if (index > 0) {
                    builder.append(',');
                }
                builder.append(quote(value.get(index)));
            }
            return builder.append(']').toString();
        }

        /**
         * 执行 fromJsonObject 对应的 CDP 业务操作。
         */
        static Map<String, Object> fromJsonObject(String value) {
            if (value == null || value.isBlank()) {
                return Map.of();
            }
            try {
                Parser parser = new Parser(value);
                Map<String, Object> result = parser.parseObject();
                parser.ensureComplete();
                return result;
            } catch (IllegalArgumentException ignored) {
                return Map.of();
            }
        }

        /**
         * 执行 fromJsonStringArray 对应的 CDP 业务操作。
         */
        static List<String> fromJsonStringArray(String value) {
            if (value == null || value.isBlank()) {
                return List.of();
            }
            try {
                Parser parser = new Parser(value);
                List<Object> raw = parser.parseArray();
                parser.ensureComplete();
                return raw.stream().map(String::valueOf).toList();
            } catch (IllegalArgumentException ignored) {
                return List.of();
            }
        }

        /**
         * 执行 valueToJson 对应的 CDP 业务操作。
         */
        private static String valueToJson(Object value) {
            if (value == null) {
                return "null";
            }
            if (value instanceof Map<?, ?> map) {
                return mapToJson(map);
            }
            if (value instanceof Iterable<?> iterable) {
                return iterableToJson(iterable);
            }
            if (value instanceof Number || value instanceof Boolean) {
                return value.toString();
            }
            return quote(String.valueOf(value));
        }

        /**
         * 执行 mapToJson 对应的 CDP 业务操作。
         */
        private static String mapToJson(Map<?, ?> map) {
            if (map == null || map.isEmpty()) {
                return "{}";
            }
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(quote(String.valueOf(entry.getKey()))).append(':').append(valueToJson(entry.getValue()));
            }
            return builder.append('}').toString();
        }

        /**
         * 执行 iterableToJson 对应的 CDP 业务操作。
         */
        private static String iterableToJson(Iterable<?> iterable) {
            StringBuilder builder = new StringBuilder("[");
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(valueToJson(item));
            }
            return builder.append(']').toString();
        }

        /**
         * 执行 quote 对应的 CDP 业务操作。
         */
        private static String quote(String value) {
            StringBuilder builder = new StringBuilder("\"");
            String safeValue = value == null ? "" : value;
            for (int index = 0; index < safeValue.length(); index++) {
                char current = safeValue.charAt(index);
                switch (current) {
                    case '"' -> builder.append("\\\"");
                    case '\\' -> builder.append("\\\\");
                    case '\b' -> builder.append("\\b");
                    case '\f' -> builder.append("\\f");
                    case '\n' -> builder.append("\\n");
                    case '\r' -> builder.append("\\r");
                    case '\t' -> builder.append("\\t");
                    default -> builder.append(current);
                }
            }
            return builder.append('"').toString();
        }

        /**
         * 表示 Parser 的业务数据或处理组件。
         */
        private static final class Parser {
            /**
             * text。
             */
            private final String text;

            /**
             * index。
             */
            private int index;

            /**
             * 创建当前组件实例。
             */
            private Parser(String text) {
                this.text = text.trim();
            }

            /**
             * 执行 parseObject 对应的 CDP 业务操作。
             */
            private Map<String, Object> parseObject() {
                LinkedHashMap<String, Object> result = new LinkedHashMap<>();
                expect('{');
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    return Collections.unmodifiableMap(result);
                }
                while (index < text.length()) {
                    String key = parseString();
                    skipWhitespace();
                    expect(':');
                    result.put(key, parseValue());
                    skipWhitespace();
                    if (peek(',')) {
                        index++;
                        continue;
                    }
                    expect('}');
                    return Collections.unmodifiableMap(result);
                }
                throw new IllegalArgumentException("unterminated object");
            }

            /**
             * 执行 parseArray 对应的 CDP 业务操作。
             */
            private List<Object> parseArray() {
                ArrayList<Object> result = new ArrayList<>();
                expect('[');
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    return List.copyOf(result);
                }
                while (index < text.length()) {
                    result.add(parseValue());
                    skipWhitespace();
                    if (peek(',')) {
                        index++;
                        continue;
                    }
                    expect(']');
                    return List.copyOf(result);
                }
                throw new IllegalArgumentException("unterminated array");
            }

            /**
             * 执行 parseValue 对应的 CDP 业务操作。
             */
            private Object parseValue() {
                skipWhitespace();
                if (peek('"')) {
                    return parseString();
                }
                if (peek('{')) {
                    return parseObject();
                }
                if (peek('[')) {
                    return parseArray();
                }
                if (match("true")) {
                    return Boolean.TRUE;
                }
                if (match("false")) {
                    return Boolean.FALSE;
                }
                if (match("null")) {
                    return null;
                }
                return parseNumber();
            }

            /**
             * 执行 parseString 对应的 CDP 业务操作。
             */
            private String parseString() {
                expect('"');
                StringBuilder builder = new StringBuilder();
                while (index < text.length()) {
                    char current = text.charAt(index++);
                    if (current == '"') {
                        return builder.toString();
                    }
                    if (current == '\\') {
                        if (index >= text.length()) {
                            throw new IllegalArgumentException("bad escape");
                        }
                        char escaped = text.charAt(index++);
                        switch (escaped) {
                            case '"' -> builder.append('"');
                            case '\\' -> builder.append('\\');
                            case 'b' -> builder.append('\b');
                            case 'f' -> builder.append('\f');
                            case 'n' -> builder.append('\n');
                            case 'r' -> builder.append('\r');
                            case 't' -> builder.append('\t');
                            default -> builder.append(escaped);
                        }
                    } else {
                        builder.append(current);
                    }
                }
                throw new IllegalArgumentException("unterminated string");
            }

            /**
             * 执行 parseNumber 对应的 CDP 业务操作。
             */
            private Object parseNumber() {
                int start = index;
                while (index < text.length()) {
                    char current = text.charAt(index);
                    if ((current >= '0' && current <= '9') || current == '-' || current == '+' || current == '.'
                            || current == 'e' || current == 'E') {
                        index++;
                    } else {
                        break;
                    }
                }
                if (start == index) {
                    throw new IllegalArgumentException("expected value");
                }
                String number = text.substring(start, index);
                try {
                    if (number.contains(".") || number.contains("e") || number.contains("E")) {
                        return Double.valueOf(number);
                    }
                    long parsed = Long.parseLong(number);
                    if (parsed >= Integer.MIN_VALUE && parsed <= Integer.MAX_VALUE) {
                        return (int) parsed;
                    }
                    return parsed;
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("bad number", ex);
                }
            }

            /**
             * 执行 match 对应的 CDP 业务操作。
             */
            private boolean match(String value) {
                if (text.startsWith(value, index)) {
                    index += value.length();
                    return true;
                }
                return false;
            }

            /**
             * 执行 peek 对应的 CDP 业务操作。
             */
            private boolean peek(char expected) {
                return index < text.length() && text.charAt(index) == expected;
            }

            /**
             * 执行 expect 对应的 CDP 业务操作。
             */
            private void expect(char expected) {
                skipWhitespace();
                if (!peek(expected)) {
                    throw new IllegalArgumentException("expected " + expected);
                }
                index++;
            }

            /**
             * 执行 skipWhitespace 对应的 CDP 业务操作。
             */
            private void skipWhitespace() {
                while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                    index++;
                }
            }

            /**
             * 执行 ensureComplete 对应的 CDP 业务操作。
             */
            private void ensureComplete() {
                skipWhitespace();
                if (index != text.length()) {
                    throw new IllegalArgumentException("trailing content");
                }
            }
        }
    }
}
