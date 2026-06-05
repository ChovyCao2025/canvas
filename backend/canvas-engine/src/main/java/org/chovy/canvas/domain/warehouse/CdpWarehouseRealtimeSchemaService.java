package org.chovy.canvas.domain.warehouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.CdpWarehouseStreamSchemaDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseStreamSchemaMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CdpWarehouseRealtimeSchemaService {

    private static final String ROLE_SOURCE = "SOURCE";
    private static final String ROLE_SINK = "SINK";
    private static final String POLICY_BACKWARD = "BACKWARD";
    private static final String POLICY_NONE = "NONE";
    private static final String STATUS_COMPATIBLE = "COMPATIBLE";
    private static final String STATUS_BREAKING = "BREAKING";
    private static final String RUNTIME_PASS = "PASS";
    private static final String RUNTIME_WARN = "WARN";
    private static final String RUNTIME_FAIL = "FAIL";
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;
    private static final int MAX_REASON_LENGTH = 1000;

    private final CdpWarehouseStreamSchemaMapper schemaMapper;
    private final ObjectMapper objectMapper;

    public SchemaVersionView register(Long tenantId, SchemaVersionCommand command, String operator) {
        if (command == null) {
            throw new IllegalArgumentException("schema command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String pipelineKey = required(command.pipelineKey(), "pipelineKey");
        String role = role(command.schemaRole());
        String version = required(command.schemaVersion(), "schemaVersion");
        String schemaJson = required(command.schemaJson(), "schemaJson");
        String policy = policy(command.compatibilityPolicy());
        ParsedSchema current = parseSchema(schemaJson);
        CdpWarehouseStreamSchemaDO previous = schemaMapper.latestActive(scopedTenantId, pipelineKey, role);
        Compatibility compatibility = evaluateCompatibility(previous, current, policy);

        CdpWarehouseStreamSchemaDO row = new CdpWarehouseStreamSchemaDO();
        row.setTenantId(scopedTenantId);
        row.setPipelineKey(pipelineKey);
        row.setSchemaRole(role);
        row.setSchemaVersion(version);
        row.setSchemaHash(hash(schemaJson));
        row.setSchemaJson(schemaJson);
        row.setCompatibilityPolicy(policy);
        row.setCompatibilityStatus(compatibility.status());
        row.setCompatibilityReason(limit(String.join("; ", compatibility.reasons())));
        row.setActive(Boolean.FALSE.equals(command.active()) ? 0 : 1);
        row.setRegisteredBy(normalizeOperator(operator));
        schemaMapper.upsert(row);
        return toView(row);
    }

    public List<SchemaVersionView> list(Long tenantId, String pipelineKey, String schemaRole, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedPipelineKey = required(pipelineKey, "pipelineKey");
        String role = hasText(schemaRole) ? role(schemaRole) : null;
        return safeList(schemaMapper.listVersions(scopedTenantId, scopedPipelineKey, role, boundLimit(limit)))
                .stream()
                .map(this::toView)
                .toList();
    }

    public SchemaVersionView latest(Long tenantId, String pipelineKey, String schemaRole) {
        Long scopedTenantId = normalizeTenant(tenantId);
        CdpWarehouseStreamSchemaDO row = schemaMapper.latestActive(
                scopedTenantId,
                required(pipelineKey, "pipelineKey"),
                role(schemaRole));
        return row == null ? null : toView(row);
    }

    public SchemaCheckpointEvaluation evaluateCheckpoint(Long tenantId,
                                                         String pipelineKey,
                                                         String sourceSchemaVersion,
                                                         String sinkSchemaVersion) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedPipelineKey = required(pipelineKey, "pipelineKey");
        List<String> reasons = new ArrayList<>();
        String status = RUNTIME_PASS;
        status = merge(status, evaluateRole(scopedTenantId, scopedPipelineKey, ROLE_SOURCE, sourceSchemaVersion, reasons));
        status = merge(status, evaluateRole(scopedTenantId, scopedPipelineKey, ROLE_SINK, sinkSchemaVersion, reasons));
        return new SchemaCheckpointEvaluation(status, List.copyOf(reasons));
    }

    private String evaluateRole(Long tenantId,
                                String pipelineKey,
                                String role,
                                String schemaVersion,
                                List<String> reasons) {
        if (!hasText(schemaVersion)) {
            return RUNTIME_PASS;
        }
        CdpWarehouseStreamSchemaDO schema = findActiveVersion(tenantId, pipelineKey, role, schemaVersion.trim());
        if (schema == null) {
            reasons.add(role.toLowerCase(Locale.ROOT) + " schema version " + schemaVersion.trim()
                    + " is not registered");
            return RUNTIME_WARN;
        }
        if (STATUS_BREAKING.equals(schema.getCompatibilityStatus())) {
            reasons.add(role.toLowerCase(Locale.ROOT) + " schema version " + schema.getSchemaVersion()
                    + " is BREAKING: " + defaultString(schema.getCompatibilityReason(), "compatibility failed"));
            return RUNTIME_FAIL;
        }
        return RUNTIME_PASS;
    }

    private CdpWarehouseStreamSchemaDO findActiveVersion(Long tenantId,
                                                         String pipelineKey,
                                                         String role,
                                                         String version) {
        CdpWarehouseStreamSchemaDO tenantSchema =
                schemaMapper.findActiveVersion(tenantId, pipelineKey, role, version);
        if (tenantSchema != null || tenantId == 0L) {
            return tenantSchema;
        }
        return schemaMapper.findActiveVersion(0L, pipelineKey, role, version);
    }

    private Compatibility evaluateCompatibility(CdpWarehouseStreamSchemaDO previous,
                                                ParsedSchema current,
                                                String policy) {
        if (POLICY_NONE.equals(policy) || previous == null) {
            return new Compatibility(STATUS_COMPATIBLE, List.of());
        }
        ParsedSchema prior = parseSchema(previous.getSchemaJson());
        List<String> reasons = new ArrayList<>();
        for (Map.Entry<String, SchemaField> entry : prior.fields().entrySet()) {
            SchemaField priorField = entry.getValue();
            SchemaField currentField = current.fields().get(entry.getKey());
            if (currentField == null) {
                reasons.add("field removed: " + priorField.name());
                continue;
            }
            if (!priorField.type().equals(currentField.type())) {
                reasons.add("field type changed: " + priorField.name() + " "
                        + priorField.type() + " -> " + currentField.type());
            }
        }
        for (Map.Entry<String, SchemaField> entry : current.fields().entrySet()) {
            if (!prior.fields().containsKey(entry.getKey()) && !entry.getValue().nullable()) {
                reasons.add("new non-null field: " + entry.getValue().name());
            }
        }
        return reasons.isEmpty()
                ? new Compatibility(STATUS_COMPATIBLE, List.of())
                : new Compatibility(STATUS_BREAKING, reasons);
    }

    private ParsedSchema parseSchema(String schemaJson) {
        try {
            JsonNode root = objectMapper.readTree(schemaJson);
            JsonNode fields = root.get("fields");
            if (fields == null || !fields.isArray()) {
                throw new IllegalArgumentException("schemaJson fields array is required");
            }
            Map<String, SchemaField> parsed = new LinkedHashMap<>();
            for (JsonNode field : fields) {
                String name = text(field, "name");
                String type = text(field, "type").toUpperCase(Locale.ROOT);
                boolean nullable = !field.has("nullable") || field.get("nullable").asBoolean();
                parsed.put(name.toLowerCase(Locale.ROOT), new SchemaField(name, type, nullable));
            }
            if (parsed.isEmpty()) {
                throw new IllegalArgumentException("schemaJson fields array must not be empty");
            }
            return new ParsedSchema(parsed);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("schemaJson is invalid", ex);
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        if (value == null || value.asText().isBlank()) {
            throw new IllegalArgumentException("schema field " + fieldName + " is required");
        }
        return value.asText().trim();
    }

    private String hash(String schemaJson) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(schemaJson.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String merge(String current, String next) {
        if (RUNTIME_FAIL.equals(current) || RUNTIME_FAIL.equals(next)) {
            return RUNTIME_FAIL;
        }
        if (RUNTIME_WARN.equals(current) || RUNTIME_WARN.equals(next)) {
            return RUNTIME_WARN;
        }
        return RUNTIME_PASS;
    }

    private SchemaVersionView toView(CdpWarehouseStreamSchemaDO row) {
        return new SchemaVersionView(
                row.getId(),
                row.getTenantId(),
                row.getPipelineKey(),
                row.getSchemaRole(),
                row.getSchemaVersion(),
                row.getSchemaHash(),
                row.getSchemaJson(),
                row.getCompatibilityPolicy(),
                row.getCompatibilityStatus(),
                row.getCompatibilityReason(),
                row.getActive() == null || row.getActive() == 1,
                row.getRegisteredBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private int boundLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String role(String value) {
        String normalized = required(value, "schemaRole").toUpperCase(Locale.ROOT);
        if (!ROLE_SOURCE.equals(normalized) && !ROLE_SINK.equals(normalized)) {
            throw new IllegalArgumentException("schemaRole must be SOURCE or SINK");
        }
        return normalized;
    }

    private String policy(String value) {
        String normalized = hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : POLICY_BACKWARD;
        if (!POLICY_BACKWARD.equals(normalized) && !POLICY_NONE.equals(normalized)) {
            throw new IllegalArgumentException("compatibilityPolicy must be BACKWARD or NONE");
        }
        return normalized;
    }

    private String required(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String normalizeOperator(String operator) {
        return hasText(operator) ? operator.trim() : "operator";
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String defaultString(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
    }

    private String limit(String value) {
        if (value == null || value.length() <= MAX_REASON_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_REASON_LENGTH);
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ParsedSchema(Map<String, SchemaField> fields) {
    }

    private record SchemaField(String name, String type, boolean nullable) {
    }

    private record Compatibility(String status, List<String> reasons) {
    }

    public record SchemaVersionCommand(
            String pipelineKey,
            String schemaRole,
            String schemaVersion,
            String schemaJson,
            String compatibilityPolicy,
            Boolean active) {
    }

    public record SchemaVersionView(
            Long id,
            Long tenantId,
            String pipelineKey,
            String schemaRole,
            String schemaVersion,
            String schemaHash,
            String schemaJson,
            String compatibilityPolicy,
            String compatibilityStatus,
            String compatibilityReason,
            boolean active,
            String registeredBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record SchemaCheckpointEvaluation(String status, List<String> reasons) {
    }
}
