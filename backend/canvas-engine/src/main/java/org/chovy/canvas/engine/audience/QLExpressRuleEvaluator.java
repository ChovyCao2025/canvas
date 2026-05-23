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

    @SuppressWarnings("unchecked")
    private String toScript(Map<String, Object> rule) {
        String logic = String.valueOf(rule.getOrDefault("logic", "AND"));
        String joinOp = "OR".equalsIgnoreCase(logic) ? " || " : " && ";
        List<String> parts = new ArrayList<>();

        List<Map<String, Object>> conditions = (List<Map<String, Object>>) rule.get("conditions");
        if (conditions != null) {
            for (Map<String, Object> condition : conditions) {
                parts.add(toConditionScript(condition));
            }
        }

        List<Map<String, Object>> groups = (List<Map<String, Object>>) rule.get("groups");
        if (groups != null) {
            for (Map<String, Object> group : groups) {
                parts.add("(" + toScript(group) + ")");
            }
        }

        return parts.isEmpty() ? "true" : String.join(joinOp, parts);
    }

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
                String listScript = list.stream().map(this::quoteIfString).collect(Collectors.joining(","));
                yield "contains([" + listScript + "], " + field + ")";
            }
            default -> "false";
        };
    }

    private String quoteIfString(Object value) {
        if (value instanceof String text) {
            return '"' + text.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
        }
        return String.valueOf(value);
    }
}
