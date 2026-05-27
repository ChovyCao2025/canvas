package org.chovy.canvas.engine.audience;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rule Evaluator Fail Closed 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
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
