package org.chovy.canvas.engine.audience;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 规则 JSON 转 SQL WHERE 片段生成器。
 *
 * <p>用于批处理人群计算，把规则下推给数据库执行。
 */
@Component
@RequiredArgsConstructor
public class SqlWhereGenerator {

    /** 规则 JSON 解析器。 */
    private final ObjectMapper objectMapper;

    /** 生成 SQL 片段与参数集合。 */
    public SqlWhere generate(String ruleJson) throws Exception {
        Map<String, Object> rule = objectMapper.readValue(ruleJson, new TypeReference<>() {});
        AtomicInteger counter = new AtomicInteger();
        MapSqlParameterSource params = new MapSqlParameterSource();
        // 规则树只生成 WHERE 片段，外层 SELECT/FROM 由数据源配置决定。
        String sql = buildGroup(rule, params, counter);
        return new SqlWhere(sql.isBlank() ? "1=1" : sql, params);
    }

    /**
     * 构建、解析或转换 build Group 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param group group 方法执行所需的业务参数
     * @param params params 方法执行所需的业务参数
     * @param counter counter 数量、阈值或分页参数
     * @return 转换或查询得到的字符串结果
     */
    @SuppressWarnings("unchecked")
    private String buildGroup(Map<String, Object> group,
                              MapSqlParameterSource params,
                              AtomicInteger counter) {
        // 组内条件按 logic 拼接，并递归处理子分组
        String logic = String.valueOf(group.getOrDefault("logic", "AND"));
        String joiner = "OR".equalsIgnoreCase(logic) ? " OR " : " AND ";
        List<String> parts = new ArrayList<>();

        Object conditionsObj = group.get("conditions");
        if (conditionsObj instanceof List<?> conditions) {
            for (Object item : conditions) {
                if (item instanceof Map<?, ?> condition) {
                    // 每个叶子条件都会绑定独立命名参数，避免不同字段/分支参数名冲突。
                    parts.add(buildCondition((Map<String, Object>) condition, params, counter));
                }
            }
        }

        Object groupsObj = group.get("groups");
        if (groupsObj instanceof List<?> groups) {
            for (Object item : groups) {
                if (item instanceof Map<?, ?> nested) {
                    String nestedSql = buildGroup((Map<String, Object>) nested, params, counter);
                    if (!nestedSql.isBlank()) {
                        // 子分组加括号保留前端规则树里的逻辑优先级。
                        parts.add("(" + nestedSql + ")");
                    }
                }
            }
        }

        return String.join(joiner, parts);
    }

    /**
     * 构建、解析或转换 build Condition 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param condition condition 方法执行所需的业务参数
     * @param params params 方法执行所需的业务参数
     * @param counter counter 数量、阈值或分页参数
     * @return 转换或查询得到的字符串结果
     */
    private String buildCondition(Map<String, Object> condition,
                                  MapSqlParameterSource params,
                                  AtomicInteger counter) {
        // 字段名做白名单校验，防止通过规则注入非法 SQL 标识符
        String field = sanitizeIdentifier(String.valueOf(condition.get("field")));
        String op = String.valueOf(condition.get("op"));
        Object value = condition.get("value");
        String paramName = "p" + counter.incrementAndGet();

        // 运算符白名单决定可生成的 SQL 形态，不允许规则 JSON 直接拼接任意 SQL。
        return switch (op) {
            case "=" -> bindSimple(field, "=", paramName, value, params);
            case "!=" -> bindSimple(field, "<>", paramName, value, params);
            case ">" -> bindSimple(field, ">", paramName, value, params);
            case ">=" -> bindSimple(field, ">=", paramName, value, params);
            case "<" -> bindSimple(field, "<", paramName, value, params);
            case "<=" -> bindSimple(field, "<=", paramName, value, params);
            case "IN" -> bindIn(field, paramName, value, params);
            default -> throw new IllegalArgumentException("Unsupported operator: " + op);
        };
    }

    /**
     * 执行 bind Simple 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param field field 方法执行所需的业务参数
     * @param sqlOp sqlOp 方法执行所需的业务参数
     * @param paramName paramName 方法执行所需的业务参数
     * @param value value 待写入、比较或转换的业务值
     * @param params params 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
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
     * 执行 bind In 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param field field 方法执行所需的业务参数
     * @param paramName paramName 方法执行所需的业务参数
     * @param value value 待写入、比较或转换的业务值
     * @param params params 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    private String bindIn(String field,
                          String paramName,
                          Object value,
                          MapSqlParameterSource params) {
        // IN 空集合等价于“不可能命中”
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return "1=0";
        }
        params.addValue(paramName, list);
        return field + " IN (:" + paramName + ')';
    }

    /**
     * 执行 sanitize Identifier 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param field field 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    private String sanitizeIdentifier(String field) {
        if (!field.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Illegal field name: " + field);
        }
        return field;
    }

    public record SqlWhere(
            /** WHERE 片段，不包含 WHERE 关键字。 */
            String sql,
            /** 与命名参数占位符对应的参数集合。 */
            MapSqlParameterSource params
    ) {
        // sql: 仅 WHERE 片段（不含 "WHERE" 关键字）
        // params: 与命名参数占位符一一对应
    }
}
