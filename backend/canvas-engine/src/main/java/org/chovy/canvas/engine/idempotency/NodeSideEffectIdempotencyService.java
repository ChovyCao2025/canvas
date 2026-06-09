package org.chovy.canvas.engine.idempotency;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
/**
 * NodeSideEffectIdempotencyService 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
 */
public class NodeSideEffectIdempotencyService {

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final int maxAttempts;

    /**
     * 初始化 NodeSideEffectIdempotencyService 实例。
     *
     * @param jdbcTemplate jdbc template 参数，用于 NodeSideEffectIdempotencyService 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param maxAttempts max attempts 参数，用于 NodeSideEffectIdempotencyService 流程中的校验、计算或对象转换。
     */
    public NodeSideEffectIdempotencyService(JdbcTemplate jdbcTemplate,
                                            ObjectMapper objectMapper,
                                            @Value("${canvas.execution.side-effect-idempotency.max-attempts:3}") int maxAttempts) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    @Transactional
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param ctx ctx 参数，用于 reserve 流程中的校验、计算或对象转换。
     * @param nodeId 业务对象 ID，用于定位具体记录。
     * @param nodeType 类型标识，用于选择对应处理分支。
     * @param operationKey 业务键，用于在同一租户下定位资源。
     * @return 返回 reserve 流程生成的业务结果。
     */
    public ReserveResult reserve(ExecutionContext ctx, String nodeId, String nodeType, String operationKey) {
        Objects.requireNonNull(ctx, "ctx must not be null");
        String key = buildKey(ctx, nodeId, nodeType, operationKey);
        Optional<NodeSideEffectRecord> existing = findByKey(normalizeTenantId(ctx.getTenantId()), key);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (existing.isPresent()) {
            NodeSideEffectRecord record = existing.get();
            if (STATUS_COMPLETED.equals(record.getStatus())) {
                return ReserveResult.completed(record, outputMap(record.getOutputJson()));
            }
            if (record.getAttemptCount() >= maxAttempts) {
                return ReserveResult.exhausted(record);
            }
            markRunning(record.getId());
            return findById(record.getId())
                    // 遍历候选数据并按业务规则筛选、转换或聚合。
                    .map(ReserveResult::reserved)
                    .orElseGet(() -> ReserveResult.reserved(record));
        }

        try {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            Long id = insertRunning(ctx, nodeId, nodeType, operationKey, key);
            return findById(id)
                    .map(ReserveResult::reserved)
                    .orElseThrow(() -> new IllegalStateException("side-effect reservation was not readable"));
        } catch (DuplicateKeyException duplicate) {
            return reserve(ctx, nodeId, nodeType, operationKey);
        }
    }

    @Transactional
    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param recordId 业务对象 ID，用于定位具体记录。
     * @param MapString map string 参数，用于 complete 流程中的校验、计算或对象转换。
     * @param output output 参数，用于 complete 流程中的校验、计算或对象转换。
     */
    public void complete(Long recordId, Map<String, Object> output) {
        jdbcTemplate.update("""
                UPDATE node_side_effect_idempotency
                SET status = ?, output_json = ?, error_message = NULL, updated_at = ?
                WHERE id = ?
                """, STATUS_COMPLETED, toJson(output == null ? Map.of() : output), LocalDateTime.now(), recordId);
    }

    @Transactional
    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param recordId 业务对象 ID，用于定位具体记录。
     * @param errorMessage error message 参数，用于 fail 流程中的校验、计算或对象转换。
     */
    public void fail(Long recordId, String errorMessage) {
        jdbcTemplate.update("""
                UPDATE node_side_effect_idempotency
                SET status = ?, error_message = ?, updated_at = ?
                WHERE id = ?
                """, STATUS_FAILED, truncate(errorMessage, 500), LocalDateTime.now(), recordId);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param idempotencyKey 业务键，用于在同一租户下定位资源。
     * @return 返回 cachedOutput 流程生成的业务结果。
     */
    public Optional<Map<String, Object>> cachedOutput(String idempotencyKey) {
        return findByRawKey(idempotencyKey)
                .filter(record -> STATUS_COMPLETED.equals(record.getStatus()))
                .map(record -> outputMap(record.getOutputJson()));
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param ctx ctx 参数，用于 buildKey 流程中的校验、计算或对象转换。
     * @param nodeId 业务对象 ID，用于定位具体记录。
     * @param nodeType 类型标识，用于选择对应处理分支。
     * @param operationKey 业务键，用于在同一租户下定位资源。
     * @return 返回组装或转换后的结果对象。
     */
    public String buildKey(ExecutionContext ctx, String nodeId, String nodeType, String operationKey) {
        String raw = normalizeTenantId(ctx.getTenantId()) + "|"
                + nullToBlank(ctx.getExecutionId()) + "|"
                + nullToBlank(ctx.getCanvasId()) + "|"
                + nullToBlank(nodeId) + "|"
                + nullToBlank(nodeType) + "|"
                + nullToBlank(operationKey);
        return sha256(raw);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @return 返回符合条件的数据列表或视图。
     */
    public Optional<NodeSideEffectRecord> findById(Long id) {
        return jdbcTemplate.query("""
                SELECT * FROM node_side_effect_idempotency
                WHERE id = ?
                LIMIT 1
                """, rowMapper(), id).stream().findFirst();
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    private Optional<NodeSideEffectRecord> findByKey(Long tenantId, String key) {
        return jdbcTemplate.query("""
                SELECT * FROM node_side_effect_idempotency
                WHERE tenant_id = ? AND idempotency_key = ?
                LIMIT 1
                """, rowMapper(), tenantId, key).stream().findFirst();
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    private Optional<NodeSideEffectRecord> findByRawKey(String key) {
        return jdbcTemplate.query("""
                SELECT * FROM node_side_effect_idempotency
                WHERE idempotency_key = ?
                LIMIT 1
                """, rowMapper(), key).stream().findFirst();
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param ctx ctx 参数，用于 insertRunning 流程中的校验、计算或对象转换。
     * @param nodeId 业务对象 ID，用于定位具体记录。
     * @param nodeType 类型标识，用于选择对应处理分支。
     * @param operationKey 业务键，用于在同一租户下定位资源。
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回 insert running 计算得到的数量、金额或指标值。
     */
    private Long insertRunning(ExecutionContext ctx, String nodeId, String nodeType, String operationKey, String key) {
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO node_side_effect_idempotency
                    (tenant_id, execution_id, canvas_id, node_id, node_type, operation_key,
                     idempotency_key, status, attempt_count, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, normalizeTenantId(ctx.getTenantId()));
            ps.setString(2, ctx.getExecutionId());
            ps.setLong(3, ctx.getCanvasId());
            ps.setString(4, nodeId);
            ps.setString(5, nodeType);
            ps.setString(6, operationKey == null ? "" : operationKey);
            ps.setString(7, key);
            ps.setString(8, STATUS_RUNNING);
            ps.setTimestamp(9, Timestamp.valueOf(now));
            ps.setTimestamp(10, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);
        Number keyValue = keyHolder.getKey();
        if (keyValue != null) {
            return keyValue.longValue();
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return findByKey(normalizeTenantId(ctx.getTenantId()), key)
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .map(NodeSideEffectRecord::getId)
                .orElseThrow(() -> new IllegalStateException("side-effect reservation did not return a key"));
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param recordId 业务对象 ID，用于定位具体记录。
     */
    private void markRunning(Long recordId) {
        jdbcTemplate.update("""
                UPDATE node_side_effect_idempotency
                SET status = ?, attempt_count = attempt_count + 1, error_message = NULL, updated_at = ?
                WHERE id = ? AND status <> ?
                """, STATUS_RUNNING, LocalDateTime.now(), recordId, STATUS_COMPLETED);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回 outputMap 流程生成的业务结果。
     */
    private Map<String, Object> outputMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            throw new IllegalArgumentException("side-effect output JSON is invalid", e);
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("side-effect output serialization failed", e);
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @return 返回 row mapper 汇总后的集合、分页或映射视图。
     */
    private RowMapper<NodeSideEffectRecord> rowMapper() {
        return (rs, rowNum) -> NodeSideEffectRecord.builder()
                .id(rs.getLong("id"))
                .tenantId(rs.getLong("tenant_id"))
                .executionId(rs.getString("execution_id"))
                .canvasId(rs.getLong("canvas_id"))
                .nodeId(rs.getString("node_id"))
                .nodeType(rs.getString("node_type"))
                .operationKey(rs.getString("operation_key"))
                .idempotencyKey(rs.getString("idempotency_key"))
                .status(rs.getString("status"))
                .attemptCount(rs.getInt("attempt_count"))
                .outputJson(rs.getString("output_json"))
                .errorMessage(rs.getString("error_message"))
                .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
                .updatedAt(toLocalDateTime(rs.getTimestamp("updated_at")))
                .build();
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param timestamp 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回组装或转换后的结果对象。
     */
    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenantId(Long tenantId) {
        return tenantId == null ? 1L : tenantId;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param raw raw 参数，用于 sha256 流程中的校验、计算或对象转换。
     * @return 返回 sha256 生成的文本或业务键。
     */
    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 null to blank 生成的文本或业务键。
     */
    private String nullToBlank(Object value) {
        return value == null ? "" : value.toString();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param maxLength max length 参数，用于 truncate 流程中的校验、计算或对象转换。
     * @return 返回 truncate 生成的文本或业务键。
     */
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /**
     * ReserveResult 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public record ReserveResult(
            boolean reserved,
            boolean completed,
            boolean exhausted,
            NodeSideEffectRecord record,
            Map<String, Object> cachedOutput
    ) {
        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param record record 参数，用于 reserved 流程中的校验、计算或对象转换。
         * @return 返回 reserved 流程生成的业务结果。
         */
        static ReserveResult reserved(NodeSideEffectRecord record) {
            return new ReserveResult(true, false, false, record, Map.of());
        }

        /**
         * 推进状态流转并记录本次处理结果。
         *
         * @param record record 参数，用于 completed 流程中的校验、计算或对象转换。
         * @param MapString map string 参数，用于 completed 流程中的校验、计算或对象转换。
         * @param cachedOutput 依赖组件，用于完成数据访问、计算或外部能力调用。
         * @return 返回 completed 流程生成的业务结果。
         */
        static ReserveResult completed(NodeSideEffectRecord record, Map<String, Object> cachedOutput) {
            return new ReserveResult(false, true, false, record, cachedOutput == null ? Map.of() : cachedOutput);
        }

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param record record 参数，用于 exhausted 流程中的校验、计算或对象转换。
         * @return 返回 exhausted 流程生成的业务结果。
         */
        static ReserveResult exhausted(NodeSideEffectRecord record) {
            return new ReserveResult(false, false, true, record, Map.of());
        }

        /**
         * 校验输入、权限或业务前置条件。
         *
         * @return 返回布尔判断结果。
         */
        public boolean shouldExecute() {
            return reserved;
        }
    }
}
