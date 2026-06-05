package org.chovy.canvas.infrastructure.bi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryItem;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryReader;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class JdbcBiQueryHistoryReader implements BiQueryHistoryReader {

    private static final int MAX_LIMIT = 100;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcBiQueryHistoryReader(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<BiQueryHistoryItem> recent(Long tenantId, int limit) {
        int boundedLimit = boundLimit(limit);
        return jdbcTemplate.query("""
                        SELECT id, user_id, request_json, compiled_sql_hash, row_count,
                               duration_ms, status, error_message, created_at
                        FROM bi_query_history
                        WHERE tenant_id = ?
                        ORDER BY created_at DESC, id DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> toItem(rs),
                tenantId == null ? 0L : tenantId,
                boundedLimit);
    }

    private BiQueryHistoryItem toItem(ResultSet rs) throws SQLException {
        return new BiQueryHistoryItem(
                rs.getLong("id"),
                datasetKey(rs.getString("request_json")),
                rs.getString("user_id"),
                rs.getObject("row_count") == null ? 0 : rs.getInt("row_count"),
                rs.getObject("duration_ms") == null ? 0L : rs.getLong("duration_ms"),
                rs.getString("status"),
                rs.getString("compiled_sql_hash"),
                rs.getString("error_message"),
                toLocalDateTime(rs.getTimestamp("created_at")));
    }

    private String datasetKey(String requestJson) {
        if (requestJson == null || requestJson.isBlank()) {
            return "unknown";
        }
        try {
            return objectMapper.readValue(requestJson, BiQueryRequest.class).datasetKey();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private int boundLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
