package org.chovy.canvas.engine.audience;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RuleEvaluatorRouter {

    private final Map<String, RuleEvaluator> evaluators;

    public boolean evaluate(String engineType, String ruleJson, Map<String, Object> context) {
        RuleEvaluator evaluator = evaluators.getOrDefault(engineType, evaluators.get("AVIATOR"));
        return evaluator.evaluate(ruleJson, context);
    }
}
