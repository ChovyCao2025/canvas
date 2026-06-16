package org.chovy.canvas.risk.domain.dsl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 风控规则 DSL 解析器，将 JSON 规则转换为带类型的条件、分组和操作数节点。
 */
public class RiskRuleParser {

    /**
     * 保存 jsonCodec 对应的风控状态或配置。
     */
    private final RiskRuleJsonCodec jsonCodec;


    /**
     * 使用指定 JSON 边界构造解析器。
     */
    public RiskRuleParser(RiskRuleJsonCodec jsonCodec) {
        this.jsonCodec = Objects.requireNonNull(jsonCodec, "jsonCodec");
    }

    /**
     * 解析完整规则 JSON，并返回根规则组节点。
     */
    public RiskRuleGroupNode parse(String json) {
        return parse(jsonCodec.readTree(json));
    }

    /**
     * 解析已完成 JSON 读取的规则树。
     */
    public RiskRuleGroupNode parse(RiskRuleJsonNode root) {
        if (root == null || !root.isObject()) {
            throw new RiskRuleParseException(RiskValidationErrorCode.INVALID_JSON, "$", "root must be an object");
        }
        // 解析路径采用类似 JSONPath 的格式，方便前端定位具体出错的规则字段。
        return parseGroup(root, "$");
    }

    /**
     * 解析一个规则组，包括组逻辑、直接条件和嵌套子组。
     */
    private RiskRuleGroupNode parseGroup(RiskRuleJsonNode node, String path) {
        RiskRuleLogic logic = parseLogic(requiredText(node, "logic", path + ".logic"), path + ".logic");
        List<RiskRuleConditionNode> conditions = parseConditions(node.path("conditions"), path + ".conditions");
        List<RiskRuleGroupNode> groups = parseGroups(node.path("groups"), path + ".groups");
        return new RiskRuleGroupNode(logic, conditions, groups);
    }

    /**
     * 将 DSL 中的逻辑字符串解析为 AND/OR 枚举。
     */
    private RiskRuleLogic parseLogic(String value, String path) {
        try {
            return RiskRuleLogic.valueOf(value.toUpperCase());
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IllegalArgumentException ex) {
            throw new RiskRuleParseException(RiskValidationErrorCode.INVALID_JSON, path, "unknown logic: " + value);
        }
    }

    /**
     * 解析当前规则组下的条件数组，缺省时按空条件处理。
     */
    private List<RiskRuleConditionNode> parseConditions(RiskRuleJsonNode node, String path) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (node.isMissing() || node.isNull()) {
            return List.of();
        }
        if (!node.isArray()) {
            throw new RiskRuleParseException(RiskValidationErrorCode.INVALID_JSON, path, "conditions must be an array");
        }
        List<RiskRuleConditionNode> conditions = new ArrayList<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (int i = 0; i < node.size(); i++) {
            conditions.add(parseCondition(node.get(i), path + "[" + i + "]"));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return conditions;
    }

    /**
     * 解析单条条件，包含左操作数、操作符和右操作数。
     */
    private RiskRuleConditionNode parseCondition(RiskRuleJsonNode node, String path) {
        if (node == null || !node.isObject()) {
            throw new RiskRuleParseException(RiskValidationErrorCode.INVALID_JSON, path, "condition must be an object");
        }
        RiskRuleOperand left = parseOperand(node.get("left"), path + ".left");
        RiskRuleOperator operator = parseOperator(requiredText(node, "op", path + ".op"), path + ".op");
        // 单目操作符仍解析 right 字段，是否忽略由校验器和执行器统一判断。
        RiskRuleOperand right = parseOperand(node.get("right"), path + ".right");
        return new RiskRuleConditionNode(left, operator, right);
    }

    /**
     * 按 DSL wire value 或枚举名解析规则操作符。
     */
    private RiskRuleOperator parseOperator(String value, String path) {
        return RiskRuleOperator.fromWireValue(value)
                .orElseThrow(() -> new RiskRuleParseException(
                        RiskValidationErrorCode.UNKNOWN_OPERATOR, path, "unknown operator: " + value));
    }

    /**
     * 解析操作数，并将 FEATURE、LITERAL、LIST、CONTEXT、EVENT、SUBJECT 转为类型安全节点。
     */
    private RiskRuleOperand parseOperand(RiskRuleJsonNode node, String path) {
        if (node == null || !node.isObject()) {
            throw new RiskRuleParseException(RiskValidationErrorCode.INVALID_JSON, path, "operand must be an object");
        }
        String type = requiredText(node, "type", path + ".type").toUpperCase();
        return switch (type) {
            case "FEATURE" -> RiskRuleOperand.feature(requiredText(node, "key", path + ".key"));
            case "LITERAL" -> RiskRuleOperand.literal(literalValue(node.get("value")));
            case "LIST" -> RiskRuleOperand.list(requiredText(node, "key", path + ".key"));
            case "CONTEXT" -> RiskRuleOperand.context(requiredText(node, "path", path + ".path"));
            case "EVENT" -> RiskRuleOperand.event(requiredText(node, "path", path + ".path"));
            case "SUBJECT" -> RiskRuleOperand.subject(requiredText(node, "path", path + ".path"));
            default -> throw new RiskRuleParseException(
                    RiskValidationErrorCode.UNKNOWN_OPERAND_TYPE, path + ".type", "unknown operand type: " + type);
        };
    }

    /**
     * 将 JSON 字面量转换为运行时可比较的 Java 值。
     */
    private Object literalValue(RiskRuleJsonNode node) {
        if (node == null || node.isMissing() || node.isNull()) {
            return null;
        }
        // 字面量保留 JSON 原生语义；对象字面量不支持，避免运行时比较逻辑过度复杂。
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isIntegralNumber()) {
            return node.longValue();
        }
        if (node.isDecimalNumber()) {
            return node.decimalValue();
        }
        if (node.isText()) {
            return node.textValue();
        }
        if (node.isArray()) {
            List<Object> values = new ArrayList<>();
            for (RiskRuleJsonNode item : node.elements()) {
                values.add(literalValue(item));
            }
            return List.copyOf(values);
        }
        throw new RiskRuleParseException(RiskValidationErrorCode.INVALID_JSON, "$.literal.value", "unsupported literal value");
    }

    /**
     * 解析嵌套规则组数组，缺省时按无子组处理。
     */
    private List<RiskRuleGroupNode> parseGroups(RiskRuleJsonNode node, String path) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (node.isMissing() || node.isNull()) {
            return List.of();
        }
        if (!node.isArray()) {
            throw new RiskRuleParseException(RiskValidationErrorCode.INVALID_JSON, path, "groups must be an array");
        }
        List<RiskRuleGroupNode> groups = new ArrayList<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (int i = 0; i < node.size(); i++) {
            groups.add(parseGroup(node.get(i), path + "[" + i + "]"));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return groups;
    }

    /**
     * 读取必填文本字段，并在缺失或空白时抛出带路径的解析异常。
     */
    private String requiredText(RiskRuleJsonNode node, String field, String path) {
        RiskRuleJsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.isText() || value.textValue().isBlank()) {
            throw new RiskRuleParseException(RiskValidationErrorCode.INVALID_JSON, path, "required text field is missing");
        }
        return value.textValue();
    }
}
