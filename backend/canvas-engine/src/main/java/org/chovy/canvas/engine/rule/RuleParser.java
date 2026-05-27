package org.chovy.canvas.engine.rule;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class RuleParser {

    private final ObjectMapper objectMapper;

    public RuleGroup parseAudienceJson(String ruleJson) throws Exception {
        if (ruleJson == null || ruleJson.isBlank()) {
            return new RuleGroup(RuleLogic.AND, List.of(), false);
        }
        Map<String, Object> rule = objectMapper.readValue(ruleJson, new TypeReference<>() {});
        return parseAudienceGroup(rule);
    }

    public RuleGroup parseAudienceGroup(Map<String, Object> group) {
        if (group == null) {
            return new RuleGroup(RuleLogic.AND, List.of(), false);
        }
        RuleLogic logic = RuleLogic.parse(group.getOrDefault("logic", "AND"));
        boolean explicitMatchAll = Boolean.TRUE.equals(group.get("matchAll"));
        List<RuleNode> children = new ArrayList<>();

        Object conditionsObj = group.get("conditions");
        if (conditionsObj instanceof List<?> conditions) {
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

        return new RuleGroup(logic, children, explicitMatchAll);
    }

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

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> raw) {
        return (Map<String, Object>) raw;
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
