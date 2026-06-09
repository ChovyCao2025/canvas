package org.chovy.canvas.domain.warehouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
/**
 * CdpWarehouseRealtimeSchemaService 承载对应领域的业务规则、流程编排和结果转换。
 */
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

    /**
     * 初始化 CdpWarehouseRealtimeSchemaService 实例。
     *
     * @param schemaMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseRealtimeSchemaService(CdpWarehouseStreamSchemaMapper schemaMapper,
                                             ObjectMapper objectMapper) {
        this.schemaMapper = schemaMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 register 流程生成的业务结果。
     */
    public SchemaVersionView register(Long tenantId, SchemaVersionCommand command, String operator) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param pipelineKey 业务键，用于在同一租户下定位资源。
     * @param schemaRole schema role 参数，用于 list 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<SchemaVersionView> list(Long tenantId, String pipelineKey, String schemaRole, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedPipelineKey = required(pipelineKey, "pipelineKey");
        String role = hasText(schemaRole) ? role(schemaRole) : null;
        return safeList(schemaMapper.listVersions(scopedTenantId, scopedPipelineKey, role, boundLimit(limit)))
                .stream()
                .map(this::toView)
                .toList();
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param pipelineKey 业务键，用于在同一租户下定位资源。
     * @param schemaRole schema role 参数，用于 latest 流程中的校验、计算或对象转换。
     * @return 返回 latest 流程生成的业务结果。
     */
    public SchemaVersionView latest(Long tenantId, String pipelineKey, String schemaRole) {
        Long scopedTenantId = normalizeTenant(tenantId);
        CdpWarehouseStreamSchemaDO row = schemaMapper.latestActive(
                scopedTenantId,
                required(pipelineKey, "pipelineKey"),
                role(schemaRole));
        return row == null ? null : toView(row);
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param pipelineKey 业务键，用于在同一租户下定位资源。
     * @param sourceSchemaVersion source schema version 参数，用于 evaluateCheckpoint 流程中的校验、计算或对象转换。
     * @param sinkSchemaVersion sink schema version 参数，用于 evaluateCheckpoint 流程中的校验、计算或对象转换。
     * @return 返回 evaluate checkpoint 计算得到的数量、金额或指标值。
     */
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

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param pipelineKey 业务键，用于在同一租户下定位资源。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param schemaVersion schema version 参数，用于 evaluateRole 流程中的校验、计算或对象转换。
     * @param reasons reasons 参数，用于 evaluateRole 流程中的校验、计算或对象转换。
     * @return 返回 evaluate role 生成的文本或业务键。
     */
    private String evaluateRole(Long tenantId,
                                String pipelineKey,
                                String role,
                                String schemaVersion,
                                List<String> reasons) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return RUNTIME_PASS;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param pipelineKey 业务键，用于在同一租户下定位资源。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param version version 参数，用于 findActiveVersion 流程中的校验、计算或对象转换。
     * @return 返回符合条件的数据列表或视图。
     */
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

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param previous previous 参数，用于 evaluateCompatibility 流程中的校验、计算或对象转换。
     * @param current current 参数，用于 evaluateCompatibility 流程中的校验、计算或对象转换。
     * @param policy policy 参数，用于 evaluateCompatibility 流程中的校验、计算或对象转换。
     * @return 返回 evaluateCompatibility 流程生成的业务结果。
     */
    private Compatibility evaluateCompatibility(CdpWarehouseStreamSchemaDO previous,
                                                ParsedSchema current,
                                                String policy) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (POLICY_NONE.equals(policy) || previous == null) {
            return new Compatibility(STATUS_COMPATIBLE, List.of());
        }
        ParsedSchema prior = parseSchema(previous.getSchemaJson());
        List<String> reasons = new ArrayList<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return reasons.isEmpty()
                ? new Compatibility(STATUS_COMPATIBLE, List.of())
                : new Compatibility(STATUS_BREAKING, reasons);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param schemaJson JSON 字符串，承载结构化配置或明细。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private ParsedSchema parseSchema(String schemaJson) {
        try {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            JsonNode root = objectMapper.readTree(schemaJson);
            JsonNode fields = root.get("fields");
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (fields == null || !fields.isArray()) {
                throw new IllegalArgumentException("schemaJson fields array is required");
            }
            Map<String, SchemaField> parsed = new LinkedHashMap<>();
            // 遍历候选数据并按业务规则筛选、转换或聚合。
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param node node 参数，用于 text 流程中的校验、计算或对象转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 text 生成的文本或业务键。
     */
    private String text(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        if (value == null || value.asText().isBlank()) {
            throw new IllegalArgumentException("schema field " + fieldName + " is required");
        }
        return value.asText().trim();
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param schemaJson JSON 字符串，承载结构化配置或明细。
     * @return 返回布尔判断结果。
     */
    private String hash(String schemaJson) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(schemaJson.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param current current 参数，用于 merge 流程中的校验、计算或对象转换。
     * @param next next 参数，用于 merge 流程中的校验、计算或对象转换。
     * @return 返回 merge 生成的文本或业务键。
     */
    private String merge(String current, String next) {
        if (RUNTIME_FAIL.equals(current) || RUNTIME_FAIL.equals(next)) {
            return RUNTIME_FAIL;
        }
        if (RUNTIME_WARN.equals(current) || RUNTIME_WARN.equals(next)) {
            return RUNTIME_WARN;
        }
        return RUNTIME_PASS;
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 role 生成的文本或业务键。
     */
    private String role(String value) {
        String normalized = required(value, "schemaRole").toUpperCase(Locale.ROOT);
        if (!ROLE_SOURCE.equals(normalized) && !ROLE_SINK.equals(normalized)) {
            throw new IllegalArgumentException("schemaRole must be SOURCE or SINK");
        }
        return normalized;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 policy 生成的文本或业务键。
     */
    private String policy(String value) {
        String normalized = hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : POLICY_BACKWARD;
        if (!POLICY_BACKWARD.equals(normalized) && !POLICY_NONE.equals(normalized)) {
            throw new IllegalArgumentException("compatibilityPolicy must be BACKWARD or NONE");
        }
        return normalized;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOperator(String operator) {
        return hasText(operator) ? operator.trim() : "operator";
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String limit(String value) {
        if (value == null || value.length() <= MAX_REASON_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_REASON_LENGTH);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * ParsedSchema 承载对应领域的业务规则、流程编排和结果转换。
     */
    private record ParsedSchema(Map<String, SchemaField> fields) {
    }

    /**
     * SchemaField 承载对应领域的业务规则、流程编排和结果转换。
     */
    private record SchemaField(String name, String type, boolean nullable) {
    }

    /**
     * Compatibility 承载对应领域的业务规则、流程编排和结果转换。
     */
    private record Compatibility(String status, List<String> reasons) {
    }

    /**
     * SchemaVersionCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record SchemaVersionCommand(
            String pipelineKey,
            String schemaRole,
            String schemaVersion,
            String schemaJson,
            String compatibilityPolicy,
            Boolean active) {
    }

    /**
     * SchemaVersionView 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * SchemaCheckpointEvaluation 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record SchemaCheckpointEvaluation(String status, List<String> reasons) {
    }
}
