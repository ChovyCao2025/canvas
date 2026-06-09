package org.chovy.canvas.domain.cdp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.aviator.AviatorEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
/**
 * CdpRuleEvaluator 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpRuleEvaluator {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    /**
     * Evaluation 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record Evaluation(boolean matched, Object value) {
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param expressionJson JSON 字符串，承载结构化配置或明细。
     * @param MapString map string 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @param properties 配置对象，用于控制运行参数和策略开关。
     * @return 返回 evaluate 流程生成的业务结果。
     */
    public Evaluation evaluate(String expressionJson, Map<String, Object> properties) {
        return evaluate(expressionJson, "RULE", properties);
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param expressionJson JSON 字符串，承载结构化配置或明细。
     * @param computeType 类型标识，用于选择对应处理分支。
     * @param MapString map string 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @param properties 配置对象，用于控制运行参数和策略开关。
     * @return 返回 evaluate 流程生成的业务结果。
     */
    public Evaluation evaluate(String expressionJson, String computeType, Map<String, Object> properties) {
        if ("EXPR".equalsIgnoreCase(computeType)) {
            return evaluateExpression(expressionJson, properties);
        }
        Map<String, Object> expression = readExpression(expressionJson);
        String field = requireText((String) expression.get("field"), "field");
        String op = requireText((String) expression.get("op"), "op");
        Object actual = readPath(properties, field);
        Object expected = expression.get("value");
        boolean matched = compare(actual, op, expected);
        Object value = matched ? expression.getOrDefault("then", Boolean.TRUE) : null;
        return new Evaluation(matched, value);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param expressionJson JSON 字符串，承载结构化配置或明细。
     */
    public void validate(String expressionJson) {
        validate(expressionJson, "RULE");
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param expressionJson JSON 字符串，承载结构化配置或明细。
     * @param computeType 类型标识，用于选择对应处理分支。
     */
    public void validate(String expressionJson, String computeType) {
        if ("EXPR".equalsIgnoreCase(computeType)) {
            String expr = readExpressionText(expressionJson);
            AviatorEvaluator.compile(expr, true);
            return;
        }
        Map<String, Object> expression = readExpression(expressionJson);
        requireText((String) expression.get("field"), "field");
        String op = requireText((String) expression.get("op"), "op");
        if (!isSupportedOperator(op)) {
            throw new IllegalArgumentException("unsupported rule operator: " + op);
        }
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param propertiesJson JSON 字符串，承载结构化配置或明细。
     * @return 返回 readProperties 流程生成的业务结果。
     */
    public Map<String, Object> readProperties(String propertiesJson) {
        if (propertiesJson == null || propertiesJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(propertiesJson, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("profile properties JSON is invalid", e);
        }
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param MapString map string 参数，用于 writeProperties 流程中的校验、计算或对象转换。
     * @param properties 配置对象，用于控制运行参数和策略开关。
     * @return 返回 write properties 生成的文本或业务键。
     */
    public String writeProperties(Map<String, Object> properties) {
        try {
            return objectMapper.writeValueAsString(properties);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("profile properties JSON is invalid", e);
        }
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param expressionJson JSON 字符串，承载结构化配置或明细。
     * @return 返回 readExpression 流程生成的业务结果。
     */
    private Map<String, Object> readExpression(String expressionJson) {
        if (expressionJson == null || expressionJson.isBlank()) {
            throw new IllegalArgumentException("expressionJson cannot be blank");
        }
        try {
            return objectMapper.readValue(expressionJson, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("expressionJson is invalid", e);
        }
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param expressionJson JSON 字符串，承载结构化配置或明细。
     * @param MapString map string 参数，用于 evaluateExpression 流程中的校验、计算或对象转换。
     * @param properties 配置对象，用于控制运行参数和策略开关。
     * @return 返回 evaluateExpression 流程生成的业务结果。
     */
    private Evaluation evaluateExpression(String expressionJson, Map<String, Object> properties) {
        Object value = AviatorEvaluator.execute(readExpressionText(expressionJson), properties);
        return new Evaluation(value != null, value);
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param expressionJson JSON 字符串，承载结构化配置或明细。
     * @return 返回 read expression text 生成的文本或业务键。
     */
    private String readExpressionText(String expressionJson) {
        Map<String, Object> expression = readExpression(expressionJson);
        return requireText((String) expression.get("expr"), "expr");
    }

    @SuppressWarnings("unchecked")
    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param MapString map string 参数，用于 readPath 流程中的校验、计算或对象转换。
     * @param properties 配置对象，用于控制运行参数和策略开关。
     * @param path path 参数，用于 readPath 流程中的校验、计算或对象转换。
     * @return 返回 readPath 流程生成的业务结果。
     */
    private Object readPath(Map<String, Object> properties, String path) {
        Object current = properties;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> currentMap)) {
                return null;
            }
            current = ((Map<String, Object>) currentMap).get(part);
        }
        return current;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param actual actual 参数，用于 compare 流程中的校验、计算或对象转换。
     * @param op op 参数，用于 compare 流程中的校验、计算或对象转换。
     * @param expected 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 compare 的布尔判断结果。
     */
    private boolean compare(Object actual, String op, Object expected) {
        if (!isSupportedOperator(op)) {
            throw new IllegalArgumentException("unsupported rule operator: " + op);
        }
        return switch (op) {
            case ">=" -> asDouble(actual) >= asDouble(expected);
            case ">" -> asDouble(actual) > asDouble(expected);
            case "<=" -> asDouble(actual) <= asDouble(expected);
            case "<" -> asDouble(actual) < asDouble(expected);
            case "==" -> Objects.equals(String.valueOf(actual), String.valueOf(expected));
            case "!=" -> !Objects.equals(String.valueOf(actual), String.valueOf(expected));
            case "contains" -> actual != null && expected != null && String.valueOf(actual).contains(String.valueOf(expected));
            default -> false;
        };
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param op op 参数，用于 isSupportedOperator 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private boolean isSupportedOperator(String op) {
        return ">=".equals(op) || ">".equals(op) || "<=".equals(op) || "<".equals(op)
                || "==".equals(op) || "!=".equals(op) || "contains".equals(op);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 as double 计算得到的数量、金额或指标值。
     */
    private double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("rule value is not numeric: " + value);
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 require text 生成的文本或业务键。
     */
    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }
}
