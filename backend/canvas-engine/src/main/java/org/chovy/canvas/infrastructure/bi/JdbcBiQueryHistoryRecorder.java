package org.chovy.canvas.infrastructure.bi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryEntry;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryRecorder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JdbcBiQueryHistoryRecorder implements BiQueryHistoryRecorder {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcBiQueryHistoryRecorder(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void record(BiQueryHistoryEntry entry) {
        try {
            jdbcTemplate.update("""
                            INSERT INTO bi_query_history
                                (tenant_id, workspace_id, dataset_id, user_id, request_json,
                                 compiled_sql_hash, row_count, duration_ms, status, error_message)
                            VALUES (?, NULL, NULL, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    entry.tenantId(),
                    entry.username(),
                    requestJson(entry),
                    entry.sqlHash(),
                    entry.rowCount(),
                    entry.durationMs(),
                    entry.status(),
                    truncate(entry.errorMessage(), 1000));
        } catch (RuntimeException e) {
            log.warn("[BI_QUERY_HISTORY] record failed dataset={} status={}: {}",
                    entry.request() == null ? null : entry.request().datasetKey(),
                    entry.status(),
                    e.getMessage());
        }
    }

    private String requestJson(BiQueryHistoryEntry entry) {
        try {
            return objectMapper.writeValueAsString(entry.request());
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
