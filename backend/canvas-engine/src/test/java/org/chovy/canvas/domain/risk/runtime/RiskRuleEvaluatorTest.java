package org.chovy.canvas.domain.risk.runtime;

import org.chovy.canvas.domain.risk.dsl.RiskRuleConditionNode;
import org.chovy.canvas.domain.risk.dsl.RiskRuleGroupNode;
import org.chovy.canvas.domain.risk.dsl.RiskRuleLogic;
import org.chovy.canvas.domain.risk.dsl.RiskRuleOperand;
import org.chovy.canvas.domain.risk.dsl.RiskRuleOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RiskRuleEvaluatorTest {

    private final RiskRuleEvaluator evaluator = new RiskRuleEvaluator();

    @ParameterizedTest
    @MethodSource("matchingOperators")
    void evaluatesSupportedOperators(RiskRuleOperator operator, Object left, Object right) {
        RiskRuleEvaluationResult result = evaluator.evaluate(
                group(condition(RiskRuleOperand.literal(left), operator, RiskRuleOperand.literal(right))),
                operand -> RiskResolvedValue.present(((RiskRuleOperand.LiteralOperand) operand).value()));

        assertThat(result.matched()).isTrue();
        assertThat(result.missingFeatures()).isEmpty();
        assertThat(result.evidence()).hasSize(1);
        assertThat(result.evidence().getFirst().matched()).isTrue();
    }

    static Stream<Arguments> matchingOperators() {
        return Stream.of(
                Arguments.of(RiskRuleOperator.EQ, "A", "A"),
                Arguments.of(RiskRuleOperator.NE, "A", "B"),
                Arguments.of(RiskRuleOperator.GT, 91, 90),
                Arguments.of(RiskRuleOperator.GTE, 90, 90),
                Arguments.of(RiskRuleOperator.LT, 89, 90),
                Arguments.of(RiskRuleOperator.LTE, 90, 90),
                Arguments.of(RiskRuleOperator.LIKE, "promo-risk-high", "risk"),
                Arguments.of(RiskRuleOperator.STARTS_WITH, "device-123", "device"),
                Arguments.of(RiskRuleOperator.ENDS_WITH, "user@example.com", "example.com"),
                Arguments.of(RiskRuleOperator.CONTAINS, List.of("vip", "fraud"), "fraud"),
                Arguments.of(RiskRuleOperator.IN, "u-1", Set.of("u-1", "u-2")),
                Arguments.of(RiskRuleOperator.NOT_IN, "u-3", Set.of("u-1", "u-2")),
                Arguments.of(RiskRuleOperator.INTERSECTS, Set.of("ip-1", "ip-2"), Set.of("ip-2", "ip-3")),
                Arguments.of(RiskRuleOperator.EXISTS, "present-value", null),
                Arguments.of(RiskRuleOperator.IS_EMPTY, List.of(), null),
                Arguments.of(RiskRuleOperator.IS_NULL, null, null)
        );
    }

    @Test
    void evaluatesNestedAndOrGroups() {
        RiskRuleGroupNode root = new RiskRuleGroupNode(
                RiskRuleLogic.AND,
                List.of(condition(RiskRuleOperand.feature("risk.score"), RiskRuleOperator.GTE, RiskRuleOperand.literal(85))),
                List.of(new RiskRuleGroupNode(
                        RiskRuleLogic.OR,
                        List.of(
                                condition(RiskRuleOperand.feature("device.change_card_1d"),
                                        RiskRuleOperator.GT,
                                        RiskRuleOperand.literal(2)),
                                condition(RiskRuleOperand.feature("buyer.fail_count_1d"),
                                        RiskRuleOperator.GTE,
                                        RiskRuleOperand.literal(3))
                        ),
                        List.of())));

        Map<String, Object> values = Map.of(
                "risk.score", 90,
                "device.change_card_1d", 1,
                "buyer.fail_count_1d", 3);

        RiskRuleEvaluationResult result = evaluator.evaluate(root, operand -> {
            if (operand instanceof RiskRuleOperand.FeatureOperand featureOperand) {
                return RiskResolvedValue.present(values.get(featureOperand.key()));
            }
            if (operand instanceof RiskRuleOperand.LiteralOperand literalOperand) {
                return RiskResolvedValue.present(literalOperand.value());
            }
            return RiskResolvedValue.missing();
        });

        assertThat(result.matched()).isTrue();
        assertThat(result.evidence()).hasSize(3);
    }

    @Test
    void recordsMissingFeaturesWithoutThrowing() {
        RiskRuleGroupNode root = group(condition(
                RiskRuleOperand.feature("risk.score"),
                RiskRuleOperator.GTE,
                RiskRuleOperand.literal(85)));

        RiskRuleEvaluationResult result = evaluator.evaluate(root, operand -> {
            if (operand instanceof RiskRuleOperand.LiteralOperand literalOperand) {
                return RiskResolvedValue.present(literalOperand.value());
            }
            return RiskResolvedValue.missing();
        });

        assertThat(result.matched()).isFalse();
        assertThat(result.missingFeatures()).containsExactly("risk.score");
        assertThat(result.evidence()).hasSize(1);
        assertThat(result.evidence().getFirst().matched()).isFalse();
    }

    @Test
    void resolvesListOperandThroughResolver() {
        RiskRuleGroupNode root = group(condition(
                RiskRuleOperand.subject("userId"),
                RiskRuleOperator.IN,
                RiskRuleOperand.list("blacklist.user")));

        RiskRuleEvaluationResult result = evaluator.evaluate(root, operand -> {
            if (operand instanceof RiskRuleOperand.SubjectOperand subjectOperand
                    && subjectOperand.path().equals("userId")) {
                return RiskResolvedValue.present("u-1");
            }
            if (operand instanceof RiskRuleOperand.ListOperand listOperand
                    && listOperand.key().equals("blacklist.user")) {
                return RiskResolvedValue.present(Set.of("u-1", "u-2"));
            }
            return RiskResolvedValue.missing();
        });

        assertThat(result.matched()).isTrue();
        assertThat(result.evidence().getFirst().leftValue()).isEqualTo("u-1");
    }

    @Test
    void treatsExplicitNullAsResolvedForIsNull() {
        Map<String, Object> values = new HashMap<>();
        values.put("nullableFeature", null);

        RiskRuleEvaluationResult result = evaluator.evaluate(
                group(condition(RiskRuleOperand.feature("nullableFeature"),
                        RiskRuleOperator.IS_NULL,
                        RiskRuleOperand.literal(null))),
                operand -> {
                    if (operand instanceof RiskRuleOperand.FeatureOperand featureOperand
                            && values.containsKey(featureOperand.key())) {
                        return RiskResolvedValue.present(values.get(featureOperand.key()));
                    }
                    if (operand instanceof RiskRuleOperand.LiteralOperand literalOperand) {
                        return RiskResolvedValue.present(literalOperand.value());
                    }
                    return RiskResolvedValue.missing();
                });

        assertThat(result.matched()).isTrue();
        assertThat(result.missingFeatures()).isEmpty();
    }

    private static RiskRuleGroupNode group(RiskRuleConditionNode condition) {
        return new RiskRuleGroupNode(RiskRuleLogic.AND, List.of(condition), List.of());
    }

    private static RiskRuleConditionNode condition(
            RiskRuleOperand left,
            RiskRuleOperator operator,
            RiskRuleOperand right
    ) {
        return new RiskRuleConditionNode(left, operator, right);
    }
}
