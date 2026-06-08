package org.chovy.canvas.infrastructure.bi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryEntry;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryRecorder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * JdbcBiQueryHistoryRecorder 封装 infrastructure.bi 场景的基础设施集成。
 */
@Slf4j
@Component
public class JdbcBiQueryHistoryRecorder implements BiQueryHistoryRecorder {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 创建 JdbcBiQueryHistoryRecorder 实例并注入 infrastructure.bi 场景依赖。
     * @param jdbcTemplate jdbc template 参数，用于 JdbcBiQueryHistoryRecorder 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public JdbcBiQueryHistoryRecorder(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * record 处理 infrastructure.bi 场景的业务逻辑。
     * @param entry entry 参数，用于 record 流程中的校验、计算或对象转换。
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            log.warn("[BI_QUERY_HISTORY] record failed dataset={} status={}: {}",
                    entry.request() == null ? null : entry.request().datasetKey(),
                    entry.status(),
                    e.getMessage());
        }
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param entry entry 参数，用于 requestJson 流程中的校验、计算或对象转换。
     * @return 返回 request json 生成的文本或业务键。
     */
    private String requestJson(BiQueryHistoryEntry entry) {
        try {
            return objectMapper.writeValueAsString(entry.request());
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    /**
     * 执行 truncate 流程，围绕 truncate 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param maxLength max length 参数，用于 truncate 流程中的校验、计算或对象转换。
     * @return 返回 truncate 生成的文本或业务键。
     */
    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
