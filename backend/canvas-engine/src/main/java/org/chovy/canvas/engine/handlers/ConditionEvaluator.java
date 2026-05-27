package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.rule.RuleAstEvaluator;
import org.chovy.canvas.engine.rule.RuleParser;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class ConditionEvaluator {
    private static final RuleParser RULE_PARSER = new RuleParser(new ObjectMapper());

    private ConditionEvaluator() {
    }

    static boolean evaluate(Map<String, Object> rule, ExecutionContext ctx) {
        return evaluate(rule, ctx::getContextValue);
    }

    static boolean evaluate(Map<String, Object> rule, Map<String, Object> values) {
        return evaluate(rule, values::get);
    }

    static boolean allMatch(List<Map<String, Object>> rules, ExecutionContext ctx) {
        return rules == null || rules.stream().allMatch(rule -> evaluate(rule, ctx));
    }

    static boolean allMatch(List<Map<String, Object>> rules, Map<String, Object> values) {
        return rules == null || rules.stream().allMatch(rule -> evaluate(rule, values));
    }

    private static boolean evaluate(Map<String, Object> rule, Function<String, Object> resolver) {
        if (rule == null) return false;
        try {
            return RuleAstEvaluator.matches(RULE_PARSER.parseCanvasCondition(rule), resolver);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
