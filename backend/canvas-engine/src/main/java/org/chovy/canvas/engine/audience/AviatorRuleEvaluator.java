package org.chovy.canvas.engine.audience;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.aviator.AviatorEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 Aviator 的人群规则求值器。
 *
 * <p>将规则树转成 Aviator 表达式执行，适合在线快速判断场景。
 */
@Slf4j
@Component("AVIATOR")
@RequiredArgsConstructor
public class AviatorRuleEvaluator implements RuleEvaluator {

    /** 规则 JSON 解析器。 */
    private final ObjectMapper objectMapper;

        /**
     * 执行 evaluate 对应的业务逻辑。
     *
     * <p>实现会处理 MQ 消息、路由或发送记录，影响异步触发链路。
     *
     * @param ruleJson ruleJson 方法执行所需的业务参数
     * @param context 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 判断结果，true 表示校验通过或条件成立
     */
    @Override
    public boolean evaluate(String ruleJson, Map<String, Object> context) {
        try {
            Map<String, Object> rule = objectMapper.readValue(ruleJson, new TypeReference<>() {
            });
            // 运行态上下文会在 IN 操作符场景附加临时 list 变量
            Map<String, Object> runtimeContext = new HashMap<>(context);
            String expression = toExpression(rule, runtimeContext);
            // Aviator 求值结果约定应为 Boolean
            Object result = AviatorEvaluator.execute(expression, runtimeContext);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            // 引擎异常统一按“未命中”处理，避免影响整体流程稳定性
            log.error("[AUDIENCE][AVIATOR] evaluate failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 构建、解析或转换 to Expression 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param rule rule 方法执行所需的业务参数
     * @param context 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 转换或查询得到的字符串结果
     */
    @SuppressWarnings("unchecked")
    private String toExpression(Map<String, Object> rule, Map<String, Object> context) {
        // 每个分组通过 logic 决定条件拼接关系（AND/OR）
        String logic = String.valueOf(rule.getOrDefault("logic", "AND"));
        String joinOp = "OR".equalsIgnoreCase(logic) ? " || " : " && ";
        List<String> parts = new ArrayList<>();

        List<Map<String, Object>> conditions = (List<Map<String, Object>>) rule.get("conditions");
        if (conditions != null) {
            for (Map<String, Object> condition : conditions) {
                // 叶子条件转成表达式片段，字段值仍从 runtimeContext 动态读取。
                parts.add(toConditionExpr(condition, context));
            }
        }

        List<Map<String, Object>> groups = (List<Map<String, Object>>) rule.get("groups");
        if (groups != null) {
            for (Map<String, Object> group : groups) {
                // 子分组递归生成并加括号，避免 AND/OR 混用时改变规则语义。
                parts.add("(" + toExpression(group, context) + ")");
            }
        }

        return parts.isEmpty() ? "true" : String.join(joinOp, parts);
    }

    /**
     * 构建、解析或转换 to Condition Expr 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param condition condition 方法执行所需的业务参数
     * @param context 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 转换或查询得到的字符串结果
     */
    @SuppressWarnings("unchecked")
    private String toConditionExpr(Map<String, Object> condition, Map<String, Object> context) {
        String field = String.valueOf(condition.get("field"));
        String op = String.valueOf(condition.get("op"));
        Object value = condition.get("value");

        return switch (op) {
            case "=" -> field + " == " + quoteIfString(value);
            case "!=" -> field + " != " + quoteIfString(value);
            case ">" -> field + " > " + value;
            case ">=" -> field + " >= " + value;
            case "<" -> field + " < " + value;
            case "<=" -> field + " <= " + value;
            case "IN" -> {
                String listKey = field + "_list";
                // Aviator 的 include 需要列表变量，不能直接把 Java List 字面量拼进表达式。
                context.put(listKey, value instanceof List<?> list ? list : List.of());
                // Aviator include(list, value) 语义：value 是否在 list 中
                yield "include(" + listKey + ", " + field + ")";
            }
            default -> "false";
        };
    }

    /**
     * 执行 quote If String 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 转换或查询得到的字符串结果
     */
    private String quoteIfString(Object value) {
        if (value instanceof String text) {
            return '"' + text.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
        }
        return String.valueOf(value);
    }
}
