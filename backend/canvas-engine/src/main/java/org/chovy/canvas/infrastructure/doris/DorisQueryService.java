package org.chovy.canvas.infrastructure.doris;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Doris OLAP query facade for reporting endpoints.
 *
 * <p>When Doris is disabled or temporarily unavailable, methods return empty
 * or zero DTOs so callers can fall back to MySQL aggregates without impacting
 * user-facing APIs.
 */
@Slf4j
@Service
public class DorisQueryService {

    private final JdbcTemplate dorisJdbcTemplate;

    /**
     * 初始化 DorisQueryService 实例。
     *
     * @param dorisJdbcTemplate doris jdbc template 参数，用于 DorisQueryService 流程中的校验、计算或对象转换。
     */
    public DorisQueryService(@Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate) {
        this.dorisJdbcTemplate = dorisJdbcTemplate.getIfAvailable();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 available 的布尔判断结果。
     */
    public boolean available() {
        return dorisJdbcTemplate != null;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @return 返回 get daily stats 汇总后的集合、分页或映射视图。
     */
    public List<DailyStatsDTO> getDailyStats(Long canvasId, LocalDate from, LocalDate to) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!available()) {
            return List.of();
        }
        String sql = """
                SELECT stat_date, canvas_id, canvas_name, trigger_type,
                       SUM(total_executions) AS total_executions,
                       SUM(success_count) AS success_count,
                       SUM(fail_count) AS fail_count,
                       SUM(running_count) AS running_count,
                       SUM(unique_users) AS unique_users,
                       CASE WHEN SUM(total_executions) > 0
                            THEN SUM(total_duration_ms) / SUM(total_executions)
                            ELSE 0 END AS avg_duration_ms
                FROM canvas_dws.canvas_daily_stats
                WHERE canvas_id = ?
                  AND stat_date BETWEEN ? AND ?
                GROUP BY stat_date, canvas_id, canvas_name, trigger_type
                ORDER BY stat_date
                """;
        try {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            return dorisJdbcTemplate.query(sql,
                    (rs, rowNum) -> new DailyStatsDTO(
                            rs.getDate("stat_date").toLocalDate(),
                            rs.getLong("canvas_id"),
                            rs.getString("canvas_name"),
                            rs.getString("trigger_type"),
                            rs.getLong("total_executions"),
                            rs.getLong("success_count"),
                            rs.getLong("fail_count"),
                            rs.getLong("running_count"),
                            rs.getLong("unique_users"),
                            rs.getLong("avg_duration_ms")
                    ),
                    canvasId, from, to);
        } catch (Exception e) {
            log.warn("[DORIS_QUERY] daily stats failed canvasId={} range={}..{}: {}",
                    canvasId, from, to, e.getMessage());
            // 汇总前面计算出的状态和明细，返回给调用方。
            return List.of();
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @return 返回 get daily stats 汇总后的集合、分页或映射视图。
     */
    public List<DailyStatsDTO> getDailyStats(LocalDate from, LocalDate to) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!available()) {
            return List.of();
        }
        String sql = """
                SELECT stat_date, canvas_id, canvas_name, trigger_type,
                       SUM(total_executions) AS total_executions,
                       SUM(success_count) AS success_count,
                       SUM(fail_count) AS fail_count,
                       SUM(running_count) AS running_count,
                       SUM(unique_users) AS unique_users,
                       CASE WHEN SUM(total_executions) > 0
                            THEN SUM(total_duration_ms) / SUM(total_executions)
                            ELSE 0 END AS avg_duration_ms
                FROM canvas_dws.canvas_daily_stats
                WHERE stat_date BETWEEN ? AND ?
                GROUP BY stat_date, canvas_id, canvas_name, trigger_type
                ORDER BY stat_date, canvas_id
                """;
        try {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            return dorisJdbcTemplate.query(sql,
                    (rs, rowNum) -> new DailyStatsDTO(
                            rs.getDate("stat_date").toLocalDate(),
                            rs.getLong("canvas_id"),
                            rs.getString("canvas_name"),
                            rs.getString("trigger_type"),
                            rs.getLong("total_executions"),
                            rs.getLong("success_count"),
                            rs.getLong("fail_count"),
                            rs.getLong("running_count"),
                            rs.getLong("unique_users"),
                            rs.getLong("avg_duration_ms")
                    ),
                    from, to);
        } catch (Exception e) {
            log.warn("[DORIS_QUERY] daily stats failed range={}..{}: {}", from, to, e.getMessage());
            // 汇总前面计算出的状态和明细，返回给调用方。
            return List.of();
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @return 返回 getOverviewStats 流程生成的业务结果。
     */
    public OverviewStatsDTO getOverviewStats(LocalDate from, LocalDate to) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!available()) {
            return OverviewStatsDTO.empty();
        }
        String sql = """
                SELECT SUM(total_executions) AS total_executions,
                       SUM(success_count) AS success_count,
                       SUM(fail_count) AS fail_count,
                       SUM(running_count) AS running_count,
                       SUM(unique_users) AS unique_users,
                       CASE WHEN SUM(total_executions) > 0
                            THEN SUM(total_duration_ms) / SUM(total_executions)
                            ELSE 0 END AS avg_duration_ms
                FROM canvas_dws.canvas_daily_stats
                WHERE stat_date BETWEEN ? AND ?
                """;
        try {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            return dorisJdbcTemplate.queryForObject(sql,
                    (rs, rowNum) -> new OverviewStatsDTO(
                            rs.getLong("total_executions"),
                            rs.getLong("success_count"),
                            rs.getLong("fail_count"),
                            rs.getLong("running_count"),
                            rs.getLong("unique_users"),
                            rs.getLong("avg_duration_ms")
                    ),
                    from, to);
        } catch (Exception e) {
            log.warn("[DORIS_QUERY] overview failed range={}..{}: {}", from, to, e.getMessage());
            // 汇总前面计算出的状态和明细，返回给调用方。
            return OverviewStatsDTO.empty();
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @return 返回 get node stats 汇总后的集合、分页或映射视图。
     */
    public List<NodeStatsDTO> getNodeStats(Long canvasId, LocalDate from, LocalDate to) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!available()) {
            return List.of();
        }
        String sql = """
                SELECT node_id, node_type, node_name,
                       SUM(total_entered) AS total_entered,
                       SUM(total_success) AS total_success,
                       SUM(total_failed) AS total_failed,
                       SUM(total_skipped) AS total_skipped,
                       CASE WHEN SUM(total_entered) > 0
                            THEN SUM(total_duration_ms) / SUM(total_entered)
                            ELSE 0 END AS avg_duration_ms
                FROM canvas_dws.node_daily_stats
                WHERE canvas_id = ?
                  AND stat_date BETWEEN ? AND ?
                GROUP BY node_id, node_type, node_name
                ORDER BY total_entered DESC
                """;
        try {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            return dorisJdbcTemplate.query(sql,
                    (rs, rowNum) -> new NodeStatsDTO(
                            rs.getString("node_id"),
                            rs.getString("node_type"),
                            rs.getString("node_name"),
                            rs.getLong("total_entered"),
                            rs.getLong("total_success"),
                            rs.getLong("total_failed"),
                            rs.getLong("total_skipped"),
                            rs.getLong("avg_duration_ms")
                    ),
                    canvasId, from, to);
        } catch (Exception e) {
            log.warn("[DORIS_QUERY] node stats failed canvasId={} range={}..{}: {}",
                    canvasId, from, to, e.getMessage());
            // 汇总前面计算出的状态和明细，返回给调用方。
            return List.of();
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param executionId 业务对象 ID，用于定位具体记录。
     * @return 返回 get execution trace 汇总后的集合、分页或映射视图。
     */
    public List<TraceRowDTO> getExecutionTrace(String executionId) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!available() || executionId == null || executionId.isBlank()) {
            return List.of();
        }
        String sql = """
                SELECT trace_id, tenant_id, execution_id, node_id, node_type, node_name,
                       status, input_data, output_data, error_msg,
                       started_at, finished_at, duration_ms
                FROM canvas_ods.canvas_execution_trace
                WHERE execution_id = ?
                ORDER BY started_at ASC
                """;
        try {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            return dorisJdbcTemplate.query(sql,
                    (rs, rowNum) -> new TraceRowDTO(
                            rs.getLong("trace_id"),
                            rs.getLong("tenant_id"),
                            rs.getString("execution_id"),
                            rs.getString("node_id"),
                            rs.getString("node_type"),
                            rs.getString("node_name"),
                            rs.getInt("status"),
                            rs.getString("input_data"),
                            rs.getString("output_data"),
                            rs.getString("error_msg"),
                            toLocalDateTime(rs.getTimestamp("started_at")),
                            toLocalDateTime(rs.getTimestamp("finished_at")),
                            rs.getObject("duration_ms") == null ? null : rs.getLong("duration_ms")
                    ),
                    executionId);
        } catch (Exception e) {
            log.warn("[DORIS_QUERY] execution trace failed executionId={}: {}", executionId, e.getMessage());
            // 汇总前面计算出的状态和明细，返回给调用方。
            return List.of();
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param timestamp 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回组装或转换后的结果对象。
     */
    private static LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    /**
     * OverviewStatsDTO 封装本模块的核心职责、输入输出结构和协作边界。
     */
    public record OverviewStatsDTO(
            Long totalExecutions,
            Long successCount,
            Long failCount,
            Long runningCount,
            Long uniqueUsers,
            Long avgDurationMs
    ) {
        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @return 返回 empty 流程生成的业务结果。
         */
        public static OverviewStatsDTO empty() {
            return new OverviewStatsDTO(0L, 0L, 0L, 0L, 0L, 0L);
        }
    }

    /**
     * NodeStatsDTO 封装本模块的核心职责、输入输出结构和协作边界。
     */
    public record NodeStatsDTO(
            String nodeId,
            String nodeType,
            String nodeName,
            Long totalEntered,
            Long totalSuccess,
            Long totalFailed,
            Long totalSkipped,
            Long avgDurationMs
    ) {
    }

    /**
     * TraceRowDTO 封装本模块的核心职责、输入输出结构和协作边界。
     */
    public record TraceRowDTO(
            Long traceId,
            Long tenantId,
            String executionId,
            String nodeId,
            String nodeType,
            String nodeName,
            Integer status,
            String inputData,
            String outputData,
            String errorMsg,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            Long durationMs
    ) {
    }
}
