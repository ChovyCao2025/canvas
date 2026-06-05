package org.chovy.canvas.infrastructure.doris;

import org.chovy.canvas.domain.analytics.AudienceMaterializationService;
import org.chovy.canvas.domain.analytics.BehaviorAudienceRuleCompiler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class DorisBehaviorAudienceOlapRepository implements AudienceMaterializationService.BehaviorAudienceOlapRepository {

    private static final int MAX_LIMIT = 10000;

    private final ObjectProvider<JdbcTemplate> dorisJdbcTemplate;

    public DorisBehaviorAudienceOlapRepository(@Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate) {
        this.dorisJdbcTemplate = dorisJdbcTemplate;
    }

    @Override
    public List<String> findMatchingUsers(BehaviorAudienceRuleCompiler.CompiledBehaviorAudienceQuery query) {
        JdbcTemplate jdbcTemplate = dorisJdbcTemplate.getIfAvailable();
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

    private void validate(BehaviorAudienceRuleCompiler.CompiledBehaviorAudienceQuery query) {
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

    private String filterExpression(BehaviorAudienceRuleCompiler.Filter filter, List<Object> parameters) {
        parameters.add(filter.value());
        return propertyExpression(filter.field()) + " " + operator(filter.operator()) + " ?";
    }

    private String propertyExpression(String field) {
        return switch (field) {
            case "channel" -> "channel";
            case "canvasId", "canvas_id" -> "canvas_id";
            case "nodeId", "node_id" -> "node_id";
            default -> "JSON_UNQUOTE(JSON_EXTRACT(properties_json, '$." + field + "'))";
        };
    }

    private String metricExpression(String metric) {
        return switch (metric) {
            case "COUNT" -> "COUNT(1)";
            case "LAST_SEEN_DAYS_AGO" -> "DATEDIFF(CURRENT_DATE(), MAX(event_time))";
            case "SUM_PROPERTY" -> "SUM(CAST(JSON_UNQUOTE(JSON_EXTRACT(properties_json, '$.value')) AS DOUBLE))";
            case "MAX_PROPERTY" -> "MAX(CAST(JSON_UNQUOTE(JSON_EXTRACT(properties_json, '$.value')) AS DOUBLE))";
            default -> throw new IllegalArgumentException("unsupported metric: " + metric);
        };
    }

    private String operator(String operator) {
        return switch (operator) {
            case "=", "!=", ">", ">=", "<", "<=" -> operator;
            default -> throw new IllegalArgumentException("unsupported operator: " + operator);
        };
    }
}
