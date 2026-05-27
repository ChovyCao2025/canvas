package org.chovy.canvas.engine.audience;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于 QLExpress 的人群规则求值器。
 *
 * <p>作为 Aviator 的可替代执行器，规则 JSON 语义保持一致。
 */
@Slf4j
@Component("QL")
@RequiredArgsConstructor
public class QLExpressRuleEvaluator implements RuleEvaluator {

    /** 规则 JSON 解析器。 */
    private final ObjectMapper objectMapper;
    /** QL 执行器实例（线程安全，可复用）。 */
    private final ExpressRunner runner = new ExpressRunner();

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
            // 先把规则树转成 QL 脚本，再基于上下文求值
            String script = toScript(rule);
            DefaultContext<String, Object> runtimeContext = new DefaultContext<>();
            runtimeContext.putAll(context);
            // execute 返回 Object，按布尔 true 视为命中
            Object result = runner.execute(script, runtimeContext, null, true, false);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            // 与 Aviator 保持一致：异常降级为 false
            log.error("[AUDIENCE][QL] evaluate failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 构建、解析或转换 to Script 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param rule rule 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    @SuppressWarnings("unchecked")
    private String toScript(Map<String, Object> rule) {
        String logic = String.valueOf(rule.getOrDefault("logic", "AND"));
        String joinOp = "OR".equalsIgnoreCase(logic) ? " || " : " && ";
        List<String> parts = new ArrayList<>();

        List<Map<String, Object>> conditions = (List<Map<String, Object>>) rule.get("conditions");
        if (conditions != null) {
            for (Map<String, Object> condition : conditions) {
                // 与 Aviator 保持同一套规则 JSON 语义，只替换为 QLExpress 脚本片段。
                parts.add(toConditionScript(condition));
            }
        }

        List<Map<String, Object>> groups = (List<Map<String, Object>>) rule.get("groups");
        if (groups != null) {
            for (Map<String, Object> group : groups) {
                // 子分组递归包裹，保证嵌套规则的逻辑优先级不被扁平化。
                parts.add("(" + toScript(group) + ")");
            }
        }

        return parts.isEmpty() ? "true" : String.join(joinOp, parts);
    }

    /**
     * 构建、解析或转换 to Condition Script 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param condition condition 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    private String toConditionScript(Map<String, Object> condition) {
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
                // IN 操作转为 contains(list, field) 脚本表达式
                List<?> list = value instanceof List<?> values ? values : List.of();
                // QLExpress 需要脚本文本里的数组字面量，因此列表元素在这里逐个转义。
                String listScript = list.stream().map(this::quoteIfString).collect(Collectors.joining(","));
                yield "contains([" + listScript + "], " + field + ")";
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
