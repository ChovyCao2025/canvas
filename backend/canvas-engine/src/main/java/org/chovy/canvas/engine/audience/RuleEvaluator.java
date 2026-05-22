package org.chovy.canvas.engine.audience;

import java.util.Map;

public interface RuleEvaluator {

    boolean evaluate(String ruleJson, Map<String, Object> context);
}
