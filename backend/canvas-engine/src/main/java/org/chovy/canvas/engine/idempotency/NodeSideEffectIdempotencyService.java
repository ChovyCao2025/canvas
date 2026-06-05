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
public class NodeSideEffectIdempotencyService {

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final int maxAttempts;

    public NodeSideEffectIdempotencyService(JdbcTemplate jdbcTemplate,
                                            ObjectMapper objectMapper,
                                            @Value("${canvas.execution.side-effect-idempotency.max-attempts:3}") int maxAttempts) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    @Transactional
    public ReserveResult reserve(ExecutionContext ctx, String nodeId, String nodeType, String operationKey) {
        Objects.requireNonNull(ctx, "ctx must not be null");
        String key = buildKey(ctx, nodeId, nodeType, operationKey);
        Optional<NodeSideEffectRecord> existing = findByKey(normalizeTenantId(ctx.getTenantId()), key);
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
                    .map(ReserveResult::reserved)
                    .orElseGet(() -> ReserveResult.reserved(record));
        }

        try {
            Long id = insertRunning(ctx, nodeId, nodeType, operationKey, key);
            return findById(id)
                    .map(ReserveResult::reserved)
                    .orElseThrow(() -> new IllegalStateException("side-effect reservation was not readable"));
        } catch (DuplicateKeyException duplicate) {
            return reserve(ctx, nodeId, nodeType, operationKey);
        }
    }

    @Transactional
    public void complete(Long recordId, Map<String, Object> output) {
        jdbcTemplate.update("""
                UPDATE node_side_effect_idempotency
                SET status = ?, output_json = ?, error_message = NULL, updated_at = ?
                WHERE id = ?
                """, STATUS_COMPLETED, toJson(output == null ? Map.of() : output), LocalDateTime.now(), recordId);
    }

    @Transactional
    public void fail(Long recordId, String errorMessage) {
        jdbcTemplate.update("""
                UPDATE node_side_effect_idempotency
                SET status = ?, error_message = ?, updated_at = ?
                WHERE id = ?
                """, STATUS_FAILED, truncate(errorMessage, 500), LocalDateTime.now(), recordId);
    }

    public Optional<Map<String, Object>> cachedOutput(String idempotencyKey) {
        return findByRawKey(idempotencyKey)
                .filter(record -> STATUS_COMPLETED.equals(record.getStatus()))
                .map(record -> outputMap(record.getOutputJson()));
    }

    public String buildKey(ExecutionContext ctx, String nodeId, String nodeType, String operationKey) {
        String raw = normalizeTenantId(ctx.getTenantId()) + "|"
                + nullToBlank(ctx.getExecutionId()) + "|"
                + nullToBlank(ctx.getCanvasId()) + "|"
                + nullToBlank(nodeId) + "|"
                + nullToBlank(nodeType) + "|"
                + nullToBlank(operationKey);
        return sha256(raw);
    }

    public Optional<NodeSideEffectRecord> findById(Long id) {
        return jdbcTemplate.query("""
                SELECT * FROM node_side_effect_idempotency
                WHERE id = ?
                LIMIT 1
                """, rowMapper(), id).stream().findFirst();
    }

    private Optional<NodeSideEffectRecord> findByKey(Long tenantId, String key) {
        return jdbcTemplate.query("""
                SELECT * FROM node_side_effect_idempotency
                WHERE tenant_id = ? AND idempotency_key = ?
                LIMIT 1
                """, rowMapper(), tenantId, key).stream().findFirst();
    }

    private Optional<NodeSideEffectRecord> findByRawKey(String key) {
        return jdbcTemplate.query("""
                SELECT * FROM node_side_effect_idempotency
                WHERE idempotency_key = ?
                LIMIT 1
                """, rowMapper(), key).stream().findFirst();
    }

    private Long insertRunning(ExecutionContext ctx, String nodeId, String nodeType, String operationKey, String key) {
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
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
        return findByKey(normalizeTenantId(ctx.getTenantId()), key)
                .map(NodeSideEffectRecord::getId)
                .orElseThrow(() -> new IllegalStateException("side-effect reservation did not return a key"));
    }

    private void markRunning(Long recordId) {
        jdbcTemplate.update("""
                UPDATE node_side_effect_idempotency
                SET status = ?, attempt_count = attempt_count + 1, error_message = NULL, updated_at = ?
                WHERE id = ? AND status <> ?
                """, STATUS_RUNNING, LocalDateTime.now(), recordId, STATUS_COMPLETED);
    }

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

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("side-effect output serialization failed", e);
        }
    }

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

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Long normalizeTenantId(Long tenantId) {
        return tenantId == null ? 1L : tenantId;
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String nullToBlank(Object value) {
        return value == null ? "" : value.toString();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public record ReserveResult(
            boolean reserved,
            boolean completed,
            boolean exhausted,
            NodeSideEffectRecord record,
            Map<String, Object> cachedOutput
    ) {
        static ReserveResult reserved(NodeSideEffectRecord record) {
            return new ReserveResult(true, false, false, record, Map.of());
        }

        static ReserveResult completed(NodeSideEffectRecord record, Map<String, Object> cachedOutput) {
            return new ReserveResult(false, true, false, record, cachedOutput == null ? Map.of() : cachedOutput);
        }

        static ReserveResult exhausted(NodeSideEffectRecord record) {
            return new ReserveResult(false, false, true, record, Map.of());
        }

        public boolean shouldExecute() {
            return reserved;
        }
    }
}
