package org.chovy.canvas.engine.rule;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RuleSqlCompiler {

    public SqlWhere compile(RuleGroup rule) {
        AtomicInteger counter = new AtomicInteger();
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = build(rule, params, counter);
        return new SqlWhere(sql.isBlank() ? "1=1" : sql, params);
    }

    private String build(RuleNode node, MapSqlParameterSource params, AtomicInteger counter) {
        if (node instanceof RuleGroup group) {
            List<String> parts = new ArrayList<>();
            for (RuleNode child : group.children()) {
                String childSql = build(child, params, counter);
                if (!childSql.isBlank()) {
                    parts.add(child instanceof RuleGroup ? "(" + childSql + ")" : childSql);
                }
            }
            return String.join(group.logic() == RuleLogic.OR ? " OR " : " AND ", parts);
        }
        if (node instanceof RuleCondition condition) {
            return buildCondition(condition, params, counter);
        }
        return "";
    }

    private String buildCondition(RuleCondition condition,
                                  MapSqlParameterSource params,
                                  AtomicInteger counter) {
        String field = sanitizeIdentifier(condition.field());
        String paramName = "p" + counter.incrementAndGet();
        Object value = condition.value();
        return switch (condition.operator()) {
            case EQ -> bindSimple(field, "=", paramName, value, params);
            case NEQ -> bindSimple(field, "<>", paramName, value, params);
            case GT -> bindSimple(field, ">", paramName, value, params);
            case GTE -> bindSimple(field, ">=", paramName, value, params);
            case LT -> bindSimple(field, "<", paramName, value, params);
            case LTE -> bindSimple(field, "<=", paramName, value, params);
            case IN -> bindIn(field, paramName, value, params);
            case CONTAINS -> bindLike(field, paramName, value, params);
            case EXISTS -> field + " IS NOT NULL";
            case IS_EMPTY -> "(" + field + " IS NULL OR " + field + " = '')";
        };
    }

    private String bindSimple(String field,
                              String sqlOp,
                              String paramName,
                              Object value,
                              MapSqlParameterSource params) {
        params.addValue(paramName, value);
        return field + ' ' + sqlOp + " :" + paramName;
    }

    private String bindIn(String field,
                          String paramName,
                          Object value,
                          MapSqlParameterSource params) {
        if (value instanceof Collection<?> collection && !collection.isEmpty()) {
            params.addValue(paramName, collection);
            return field + " IN (:" + paramName + ')';
        }
        if (value instanceof String text && !text.isBlank()) {
            List<String> values = java.util.Arrays.stream(text.split(","))
                    .map(String::trim)
                    .filter(item -> !item.isEmpty())
                    .toList();
            if (!values.isEmpty()) {
                params.addValue(paramName, values);
                return field + " IN (:" + paramName + ')';
            }
        }
        return "1=0";
    }

    private String bindLike(String field,
                            String paramName,
                            Object value,
                            MapSqlParameterSource params) {
        params.addValue(paramName, "%" + String.valueOf(value) + "%");
        return field + " LIKE :" + paramName;
    }

    private String sanitizeIdentifier(String field) {
        if (field == null || !field.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new RuleValidationException("Illegal field name: " + field);
        }
        return field;
    }

    public record SqlWhere(String sql, MapSqlParameterSource params) {
    }
}
