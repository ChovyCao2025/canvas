package org.chovy.canvas.engine.rule;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RuleSqlCompiler 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
 */
public class RuleSqlCompiler {

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param rule rule 参数，用于 compile 流程中的校验、计算或对象转换。
     * @return 返回 compile 流程生成的业务结果。
     */
    public SqlWhere compile(RuleGroup rule) {
        AtomicInteger counter = new AtomicInteger();
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = build(rule, params, counter);
        return new SqlWhere(sql.isBlank() ? "1=1" : sql, params);
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param node node 参数，用于 build 流程中的校验、计算或对象转换。
     * @param params params 参数，用于 build 流程中的校验、计算或对象转换。
     * @param counter counter 参数，用于 build 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private String build(RuleNode node, MapSqlParameterSource params, AtomicInteger counter) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (node instanceof RuleGroup group) {
            List<String> parts = new ArrayList<>();
            // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return "";
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param condition condition 参数，用于 buildCondition 流程中的校验、计算或对象转换。
     * @param params params 参数，用于 buildCondition 流程中的校验、计算或对象转换。
     * @param counter counter 参数，用于 buildCondition 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @param sqlOp sql op 参数，用于 bindSimple 流程中的校验、计算或对象转换。
     * @param paramName 名称文本，用于展示或唯一性校验。
     * @param value 待处理值，用于规则计算或转换。
     * @param params params 参数，用于 bindSimple 流程中的校验、计算或对象转换。
     * @return 返回 bind simple 生成的文本或业务键。
     */
    private String bindSimple(String field,
                              String sqlOp,
                              String paramName,
                              Object value,
                              MapSqlParameterSource params) {
        params.addValue(paramName, value);
        return field + ' ' + sqlOp + " :" + paramName;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @param paramName 名称文本，用于展示或唯一性校验。
     * @param value 待处理值，用于规则计算或转换。
     * @param params params 参数，用于 bindIn 流程中的校验、计算或对象转换。
     * @return 返回 bind in 生成的文本或业务键。
     */
    private String bindIn(String field,
                          String paramName,
                          Object value,
                          MapSqlParameterSource params) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value instanceof Collection<?> collection && !collection.isEmpty()) {
            params.addValue(paramName, collection);
            return field + " IN (:" + paramName + ')';
        }
        if (value instanceof String text && !text.isBlank()) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            List<String> values = java.util.Arrays.stream(text.split(","))
                    .map(String::trim)
                    .filter(item -> !item.isEmpty())
                    .toList();
            if (!values.isEmpty()) {
                params.addValue(paramName, values);
                return field + " IN (:" + paramName + ')';
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return "1=0";
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @param paramName 名称文本，用于展示或唯一性校验。
     * @param value 待处理值，用于规则计算或转换。
     * @param params params 参数，用于 bindLike 流程中的校验、计算或对象转换。
     * @return 返回 bind like 生成的文本或业务键。
     */
    private String bindLike(String field,
                            String paramName,
                            Object value,
                            MapSqlParameterSource params) {
        params.addValue(paramName, "%" + String.valueOf(value) + "%");
        return field + " LIKE :" + paramName;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 sanitize identifier 生成的文本或业务键。
     */
    private String sanitizeIdentifier(String field) {
        if (field == null || !field.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new RuleValidationException("Illegal field name: " + field);
        }
        return field;
    }

    /**
     * SqlWhere 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public record SqlWhere(String sql, MapSqlParameterSource params) {
    }
}
