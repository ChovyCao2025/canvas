package org.chovy.canvas.engine.audience;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEvaluatorFailClosedTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void aviator_unknown_operator_does_not_match() {
        AviatorRuleEvaluator evaluator = new AviatorRuleEvaluator(objectMapper);
        String ruleJson = """
                {
                  "logic":"AND",
                  "conditions":[
                    {"field":"score","op":"STARTS_WITH","value":"8"}
                  ]
                }
                """;

        boolean matched = evaluator.evaluate(ruleJson, Map.of("score", "80"));

        assertThat(matched).isFalse();
    }

    @Test
    void ql_unknown_operator_does_not_match() {
        QLExpressRuleEvaluator evaluator = new QLExpressRuleEvaluator(objectMapper);
        String ruleJson = """
                {
                  "logic":"AND",
                  "conditions":[
                    {"field":"score","op":"STARTS_WITH","value":"8"}
                  ]
                }
                """;

        boolean matched = evaluator.evaluate(ruleJson, Map.of("score", "80"));

        assertThat(matched).isFalse();
    }

    @Test
    void aviator_lowercase_in_operator_matches() {
        AviatorRuleEvaluator evaluator = new AviatorRuleEvaluator(objectMapper);
        String ruleJson = """
                {
                  "logic":"AND",
                  "conditions":[
                    {"field":"city","op":"in","value":["Beijing","Shanghai"]}
                  ]
                }
                """;

        boolean matched = evaluator.evaluate(ruleJson, Map.of("city", "Beijing"));

        assertThat(matched).isTrue();
    }

    @Test
    void ql_lowercase_in_operator_matches() {
        QLExpressRuleEvaluator evaluator = new QLExpressRuleEvaluator(objectMapper);
        String ruleJson = """
                {
                  "logic":"AND",
                  "conditions":[
                    {"field":"city","op":"in","value":["Beijing","Shanghai"]}
                  ]
                }
                """;

        boolean matched = evaluator.evaluate(ruleJson, Map.of("city", "Beijing"));

        assertThat(matched).isTrue();
    }
}
