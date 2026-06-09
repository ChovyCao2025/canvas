package org.chovy.canvas.engine.rule;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RuleParser 参与 engine.rule 场景的画布执行引擎处理。
 */
@RequiredArgsConstructor
public class RuleParser {

    private final ObjectMapper objectMapper;

    /**
     * parseAudienceJson 校验或转换 engine.rule 场景的数据。
     * @param ruleJson JSON 字符串，承载结构化配置或明细。
     * @return 返回解析、归一化或安全处理后的值。
     */
    public RuleGroup parseAudienceJson(String ruleJson) throws Exception {
        if (ruleJson == null || ruleJson.isBlank()) {
            return new RuleGroup(RuleLogic.AND, List.of(), false);
        }
        Map<String, Object> rule = objectMapper.readValue(ruleJson, new TypeReference<>() {});
        return parseAudienceGroup(rule);
    }

    /**
     * parseAudienceGroup 校验或转换 engine.rule 场景的数据。
     * @param group group 参数，用于 parseAudienceGroup 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    public RuleGroup parseAudienceGroup(Map<String, Object> group) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (group == null) {
            return new RuleGroup(RuleLogic.AND, List.of(), false);
        }
        RuleLogic logic = RuleLogic.parse(group.getOrDefault("logic", "AND"));
        boolean explicitMatchAll = Boolean.TRUE.equals(group.get("matchAll"));
        List<RuleNode> children = new ArrayList<>();

        Object conditionsObj = group.get("conditions");
        if (conditionsObj instanceof List<?> conditions) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (Object item : conditions) {
                if (item instanceof Map<?, ?> raw) {
                    children.add(parseAudienceCondition(castMap(raw)));
                }
            }
        }

        Object groupsObj = group.get("groups");
        if (groupsObj instanceof List<?> groups) {
            for (Object item : groups) {
                if (item instanceof Map<?, ?> raw) {
                    children.add(parseAudienceGroup(castMap(raw)));
                }
            }
        }

        // 汇总前面计算出的状态和明细，返回给调用方。
        return new RuleGroup(logic, children, explicitMatchAll);
    }

    /**
     * parseCanvasRules 校验或转换 engine.rule 场景的数据。
     * @param rules rules 参数，用于 parseCanvasRules 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    public RuleGroup parseCanvasRules(List<Map<String, Object>> rules) {
        if (rules == null) {
            return new RuleGroup(RuleLogic.AND, List.of(), false);
        }
        List<RuleNode> children = new ArrayList<>(rules.size());
        for (Map<String, Object> rule : rules) {
            children.add(parseCanvasCondition(rule));
        }
        return new RuleGroup(RuleLogic.AND, children, false);
    }

    /**
     * parseCanvasCondition 校验或转换 engine.rule 场景的数据。
     * @param rule rule 参数，用于 parseCanvasCondition 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    public RuleCondition parseCanvasCondition(Map<String, Object> rule) {
        if (rule == null) {
            throw new RuleValidationException("Rule condition is null");
        }
        return new RuleCondition(
                string(rule.get("field")),
                RuleOperator.parse(rule.get("operator")),
                rule.get("value")
        );
    }

    /**
     * parseAudienceCondition 校验或转换 engine.rule 场景的数据。
     * @param condition condition 参数，用于 parseAudienceCondition 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    public RuleCondition parseAudienceCondition(Map<String, Object> condition) {
        if (condition == null) {
            throw new RuleValidationException("Rule condition is null");
        }
        return new RuleCondition(
                string(condition.get("field")),
                RuleOperator.parse(condition.get("op")),
                condition.get("value")
        );
    }

    /**
     * 将通配 Map 转换为字符串键 Map。
     *
     * @param raw 原始 Map
     * @return 字符串键对象 Map
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> raw) {
        return (Map<String, Object>) raw;
    }

    /**
     * 将对象转换为规则字段字符串。
     *
     * @param value 原始值
     * @return 字符串值，null 返回空字符串
     */
    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
