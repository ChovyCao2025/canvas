package org.chovy.canvas.infrastructure.bi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryDetail;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryItem;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryReader;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JdbcBiQueryHistoryReader 封装 infrastructure.bi 场景的基础设施集成。
 */
@Component
public class JdbcBiQueryHistoryReader implements BiQueryHistoryReader {

    private static final int MAX_LIMIT = 100;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 创建 JdbcBiQueryHistoryReader 实例并注入 infrastructure.bi 场景依赖。
     * @param jdbcTemplate jdbc template 参数，用于 JdbcBiQueryHistoryReader 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public JdbcBiQueryHistoryReader(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * recent 处理 infrastructure.bi 场景的业务逻辑。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
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

    /**
     * detail 查询 infrastructure.bi 场景的业务数据。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param historyId 业务对象 ID，用于定位具体记录。
     * @return 返回 detail 流程生成的业务结果。
     */
    @Override
    public Optional<BiQueryHistoryDetail> detail(Long tenantId, Long historyId) {
        if (historyId == null || historyId <= 0) {
            return Optional.empty();
        }
        return jdbcTemplate.query("""
                        SELECT id, user_id, request_json, compiled_sql_hash, row_count,
                               duration_ms, status, error_message, created_at
                        FROM bi_query_history
                        WHERE tenant_id = ? AND id = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> toDetail(rs),
                tenantId == null ? 0L : tenantId,
                historyId)
                .stream()
                .findFirst();
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param rs rs 参数，用于 toItem 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 转换为接口返回或领域视图。
     *
     * @param rs rs 参数，用于 toDetail 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private BiQueryHistoryDetail toDetail(ResultSet rs) throws SQLException {
        BiQueryRequest request = request(rs.getString("request_json"));
        return new BiQueryHistoryDetail(
                rs.getLong("id"),
                request == null ? "unknown" : request.datasetKey(),
                rs.getString("user_id"),
                request,
                rs.getObject("row_count") == null ? 0 : rs.getInt("row_count"),
                rs.getObject("duration_ms") == null ? 0L : rs.getLong("duration_ms"),
                rs.getString("status"),
                rs.getString("compiled_sql_hash"),
                rs.getString("error_message"),
                toLocalDateTime(rs.getTimestamp("created_at")));
    }

    /**
     * 执行 datasetKey 流程，围绕 dataset key 完成校验、计算或结果组装。
     *
     * @param requestJson JSON 字符串，承载结构化配置或明细。
     * @return 返回 dataset key 生成的文本或业务键。
     */
    private String datasetKey(String requestJson) {
        if (requestJson == null || requestJson.isBlank()) {
            return "unknown";
        }
        try {
            BiQueryRequest request = request(requestJson);
            return request == null ? "unknown" : request.datasetKey();
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 执行 request 流程，围绕 request 完成校验、计算或结果组装。
     *
     * @param requestJson JSON 字符串，承载结构化配置或明细。
     * @return 返回 request 流程生成的业务结果。
     */
    private BiQueryRequest request(String requestJson) {
        if (requestJson == null || requestJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(requestJson, BiQueryRequest.class);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param timestamp 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回组装或转换后的结果对象。
     */
    private LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
