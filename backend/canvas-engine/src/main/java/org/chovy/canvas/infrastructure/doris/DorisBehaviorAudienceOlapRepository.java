package org.chovy.canvas.infrastructure.doris;

import org.chovy.canvas.domain.analytics.AudienceMaterializationService;
import org.chovy.canvas.domain.analytics.BehaviorAudienceRuleCompiler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * DorisBehaviorAudienceOlapRepository 封装 infrastructure.doris 场景的基础设施集成。
 */
@Repository
public class DorisBehaviorAudienceOlapRepository implements AudienceMaterializationService.BehaviorAudienceOlapRepository {

    private static final int MAX_LIMIT = 10000;

    private final ObjectProvider<JdbcTemplate> dorisJdbcTemplate;

    /**
     * 创建 DorisBehaviorAudienceOlapRepository 实例并注入 infrastructure.doris 场景依赖。
     * @param dorisJdbcTemplate doris jdbc template 参数，用于 DorisBehaviorAudienceOlapRepository 流程中的校验、计算或对象转换。
     */
    public DorisBehaviorAudienceOlapRepository(@Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate) {
        this.dorisJdbcTemplate = dorisJdbcTemplate;
    }

    /**
     * findMatchingUsers 查询 infrastructure.doris 场景的业务数据。
     * @param query query 参数，用于 findMatchingUsers 流程中的校验、计算或对象转换。
     * @return 返回符合条件的数据列表或视图。
     */
    @Override
    public List<String> findMatchingUsers(BehaviorAudienceRuleCompiler.CompiledBehaviorAudienceQuery query) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        JdbcTemplate jdbcTemplate = dorisJdbcTemplate.getIfAvailable();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (jdbcTemplate == null) {
            throw new IllegalStateException("Doris is disabled");
        }
        validate(query);

        List<Object> parameters = new ArrayList<>();
        parameters.add(query.tenantId());
        parameters.add(query.eventCode());
        parameters.add(query.windowDays());

        StringBuilder sql = new StringBuilder("""
                SELECT user_id
                FROM canvas_dwd.cdp_user_event_fact
                WHERE tenant_id = ?
                  AND event_code = ?
                  AND event_time >= DATE_SUB(NOW(), INTERVAL ? DAY)
                """);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (BehaviorAudienceRuleCompiler.Filter filter : query.filters()) {
            sql.append("  AND ")
                    .append(filterExpression(filter, parameters))
                    .append("\n");
        }
        sql.append("GROUP BY user_id\n")
                .append("HAVING ")
                .append(metricExpression(query.metric()))
                .append(" ")
                .append(operator(query.operator()))
                .append(" ?\n")
                .append("ORDER BY user_id\n")
                .append("LIMIT ")
                .append(query.limit());
        parameters.add(query.threshold());

        return jdbcTemplate.query(sql.toString(),
                (rs, rowNum) -> rs.getString("user_id"),
                parameters.toArray());
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param query query 参数，用于 validate 流程中的校验、计算或对象转换。
     */
    private void validate(BehaviorAudienceRuleCompiler.CompiledBehaviorAudienceQuery query) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (query == null) {
            throw new IllegalArgumentException("query is required");
        }
        if (query.tenantId() == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (query.eventCode() == null || query.eventCode().isBlank()) {
            throw new IllegalArgumentException("eventCode is required");
        }
        if (query.windowDays() <= 0) {
            throw new IllegalArgumentException("windowDays must be positive");
        }
        if (query.limit() <= 0 || query.limit() > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
    }

    /**
     * 执行 filterExpression 流程，围绕 filter expression 完成校验、计算或结果组装。
     *
     * @param filter filter 参数，用于 filterExpression 流程中的校验、计算或对象转换。
     * @param parameters parameters 参数，用于 filterExpression 流程中的校验、计算或对象转换。
     * @return 返回 filter expression 生成的文本或业务键。
     */
    private String filterExpression(BehaviorAudienceRuleCompiler.Filter filter, List<Object> parameters) {
        parameters.add(filter.value());
        return propertyExpression(filter.field()) + " " + operator(filter.operator()) + " ?";
    }

    /**
     * 执行 propertyExpression 流程，围绕 property expression 完成校验、计算或结果组装。
     *
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 property expression 生成的文本或业务键。
     */
    private String propertyExpression(String field) {
        return switch (field) {
            case "channel" -> "channel";
            case "canvasId", "canvas_id" -> "canvas_id";
            case "nodeId", "node_id" -> "node_id";
            default -> "JSON_UNQUOTE(JSON_EXTRACT(properties_json, '$." + field + "'))";
        };
    }

    /**
     * 执行 metricExpression 流程，围绕 metric expression 完成校验、计算或结果组装。
     *
     * @param metric metric 参数，用于 metricExpression 流程中的校验、计算或对象转换。
     * @return 返回 metric expression 生成的文本或业务键。
     */
    private String metricExpression(String metric) {
        return switch (metric) {
            case "COUNT" -> "COUNT(1)";
            case "LAST_SEEN_DAYS_AGO" -> "DATEDIFF(CURRENT_DATE(), MAX(event_time))";
            case "SUM_PROPERTY" -> "SUM(CAST(JSON_UNQUOTE(JSON_EXTRACT(properties_json, '$.value')) AS DOUBLE))";
            case "MAX_PROPERTY" -> "MAX(CAST(JSON_UNQUOTE(JSON_EXTRACT(properties_json, '$.value')) AS DOUBLE))";
            default -> throw new IllegalArgumentException("unsupported metric: " + metric);
        };
    }

    /**
     * 解析操作人标识。
     *
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 operator 生成的文本或业务键。
     */
    private String operator(String operator) {
        return switch (operator) {
            case "=", "!=", ">", ">=", "<", "<=" -> operator;
            default -> throw new IllegalArgumentException("unsupported operator: " + operator);
        };
    }
}
