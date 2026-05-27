package org.chovy.canvas.engine.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleEngineAstTest {

    private final RuleParser parser = new RuleParser(new ObjectMapper());
    private final RuleValidator validator = new RuleValidator();

    @Test
    void canvasAndAudienceRulesUseTheSameEvaluationSemantics() throws Exception {
        RuleGroup canvasRule = parser.parseCanvasRules(List.of(
                Map.of("field", "amount", "operator", "GT", "value", "100"),
                Map.of("field", "city", "operator", "IN", "value", "Beijing,Shanghai")
        ));
        RuleGroup audienceRule = parser.parseAudienceJson("""
                {
                  "logic":"AND",
                  "conditions":[
                    {"field":"amount","op":">","value":100},
                    {"field":"city","op":"in","value":["Beijing","Shanghai"]}
                  ]
                }
                """);

        Map<String, Object> matching = Map.of("amount", "150", "city", "Beijing");
        Map<String, Object> missing = Map.of("amount", "90", "city", "Beijing");

        assertThat(RuleAstEvaluator.matches(canvasRule, matching::get)).isTrue();
        assertThat(RuleAstEvaluator.matches(audienceRule, matching::get)).isTrue();
        assertThat(RuleAstEvaluator.matches(canvasRule, missing::get)).isFalse();
        assertThat(RuleAstEvaluator.matches(audienceRule, missing::get)).isFalse();
    }

    @Test
    void validatorRejectsEmptyRulesUnlessMatchAllIsExplicit() throws Exception {
        RuleGroup empty = parser.parseAudienceJson("""
                {"logic":"AND","conditions":[],"groups":[]}
                """);
        RuleGroup explicitMatchAll = parser.parseAudienceJson("""
                {"logic":"AND","matchAll":true,"conditions":[],"groups":[]}
                """);

        assertThatThrownBy(() -> validator.validateOrThrow(
                empty,
                RuleValidationOptions.strict("audience")))
                .isInstanceOf(RuleValidationException.class)
                .hasMessageContaining("规则不能为空");

        validator.validateOrThrow(explicitMatchAll, RuleValidationOptions.strict("audience"));
    }

    @Test
    void validatorRejectsUnknownOperatorsAndUnsafeFields() {
        assertThatThrownBy(() -> parser.parseAudienceJson("""
                {"logic":"AND","conditions":[{"field":"score","op":"STARTS_WITH","value":"8"}]}
                """))
                .isInstanceOf(RuleValidationException.class)
                .hasMessageContaining("Unsupported operator");

        RuleGroup rule = parser.parseCanvasRules(List.of(
                Map.of("field", "user_id;drop table", "operator", "EQ", "value", "1")
        ));

        assertThatThrownBy(() -> validator.validateOrThrow(
                rule,
                RuleValidationOptions.strict("canvas")))
                .isInstanceOf(RuleValidationException.class)
                .hasMessageContaining("非法字段");
    }
}
