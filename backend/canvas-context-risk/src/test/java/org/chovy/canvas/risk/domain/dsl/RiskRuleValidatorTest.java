package org.chovy.canvas.risk.domain.dsl;

import org.chovy.canvas.risk.adapter.external.JacksonRiskRuleJsonCodec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 定义 RiskRuleValidatorTest 的风控模块职责和数据契约。
 */
class RiskRuleValidatorTest {

    private final RiskRuleParser parser = new RiskRuleParser(new JacksonRiskRuleJsonCodec());
    private final RiskFactorCatalog factorCatalog = key -> switch (key) {
        case "risk.score" -> Optional.of(new RiskFactorDefinition(
                key, RiskValueType.DECIMAL, RiskFeatureAvailability.ONLINE, RiskSubjectType.USER_ID));
        case "buyer.fail_count_1d" -> Optional.of(new RiskFactorDefinition(
                key, RiskValueType.INTEGER, RiskFeatureAvailability.ONLINE, RiskSubjectType.USER_ID));
        case "offline.graph_cluster_score" -> Optional.of(new RiskFactorDefinition(
                key, RiskValueType.DECIMAL, RiskFeatureAvailability.OFFLINE_ONLY, RiskSubjectType.USER_ID));
        default -> Optional.empty();
    };
    private final RiskListCatalog listCatalog = key -> switch (key) {
        case "blacklist.user" -> Optional.of(new RiskListDefinition(
                key, RiskSubjectType.USER_ID, RiskValueType.STRING_SET));
        case "blacklist.device" -> Optional.of(new RiskListDefinition(
                key, RiskSubjectType.DEVICE_ID, RiskValueType.STRING_SET));
        default -> Optional.empty();
    };
    private final RiskRuleValidator validator = new RiskRuleValidator(factorCatalog, listCatalog);


    /**
     * 执行 rejectsUnknownFeature 相关的风控处理逻辑。
     */
    @Test
    void rejectsUnknownFeature() {
        RiskRuleValidationResult result = validator.validate(parser.parse("""
                {
                  "logic": "AND",
                  "conditions": [
                    {
                      "left": { "type": "FEATURE", "key": "unknown.feature" },
                      "op": ">=",
                      "right": { "type": "LITERAL", "value": 1 }
                    }
                  ],
                  "groups": []
                }
                """), RiskRuntimeMode.ENFORCE);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anySatisfy(error -> {
            assertThat(error.path()).isEqualTo("$.conditions[0].left.key");
            assertThat(error.code()).isEqualTo(RiskValidationErrorCode.UNKNOWN_FEATURE);
        });
    }

    /**
     * 执行 rejectsOfflineOnlyFeatureForEnforceMode 相关的风控处理逻辑。
     */
    @Test
    void rejectsOfflineOnlyFeatureForEnforceMode() {
        RiskRuleValidationResult result = validator.validate(parser.parse("""
                {
                  "logic": "AND",
                  "conditions": [
                    {
                      "left": { "type": "FEATURE", "key": "offline.graph_cluster_score" },
                      "op": ">",
                      "right": { "type": "LITERAL", "value": 70 }
                    }
                  ],
                  "groups": []
                }
                """), RiskRuntimeMode.ENFORCE);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anySatisfy(error -> {
            assertThat(error.path()).isEqualTo("$.conditions[0].left.key");
            assertThat(error.code()).isEqualTo(RiskValidationErrorCode.FEATURE_OFFLINE_ONLY);
        });
    }

    /**
     * 执行 allowsOfflineOnlyFeatureForSimulationMode 相关的风控处理逻辑。
     */
    @Test
    void allowsOfflineOnlyFeatureForSimulationMode() {
        RiskRuleValidationResult result = validator.validate(parser.parse("""
                {
                  "logic": "AND",
                  "conditions": [
                    {
                      "left": { "type": "FEATURE", "key": "offline.graph_cluster_score" },
                      "op": ">",
                      "right": { "type": "LITERAL", "value": 70 }
                    }
                  ],
                  "groups": []
                }
                """), RiskRuntimeMode.SIMULATION);

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    /**
     * 执行 rejectsListSubjectMismatch 相关的风控处理逻辑。
     */
    @Test
    void rejectsListSubjectMismatch() {
        RiskRuleValidationResult result = validator.validate(parser.parse("""
                {
                  "logic": "AND",
                  "conditions": [
                    {
                      "left": { "type": "SUBJECT", "path": "userId" },
                      "op": "IN",
                      "right": { "type": "LIST", "key": "blacklist.device" }
                    }
                  ],
                  "groups": []
                }
                """), RiskRuntimeMode.ENFORCE);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anySatisfy(error -> {
            assertThat(error.path()).isEqualTo("$.conditions[0].right.key");
            assertThat(error.code()).isEqualTo(RiskValidationErrorCode.LIST_SUBJECT_TYPE_MISMATCH);
        });
    }

    /**
     * 执行 rejectsNestingDepthGreaterThanFive 相关的风控处理逻辑。
     */
    @Test
    void rejectsNestingDepthGreaterThanFive() {
        RiskRuleGroupNode root = new RiskRuleGroupNode(RiskRuleLogic.AND, List.of(), List.of());
        for (int depth = 0; depth < 6; depth++) {
            root = new RiskRuleGroupNode(RiskRuleLogic.AND, List.of(), List.of(root));
        }

        RiskRuleValidationResult result = validator.validate(root, RiskRuntimeMode.ENFORCE);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anySatisfy(error ->
                assertThat(error.code()).isEqualTo(RiskValidationErrorCode.MAX_DEPTH_EXCEEDED));
    }

    /**
     * 执行 rejectsMoreThanOneHundredConditions 相关的风控处理逻辑。
     */
    @Test
    void rejectsMoreThanOneHundredConditions() {
        List<RiskRuleConditionNode> conditions = IntStream.range(0, 101)
                .mapToObj(index -> new RiskRuleConditionNode(
                        RiskRuleOperand.feature("buyer.fail_count_1d"),
                        RiskRuleOperator.GTE,
                        RiskRuleOperand.literal(1)))
                .collect(Collectors.toList());

        RiskRuleValidationResult result = validator.validate(
                new RiskRuleGroupNode(RiskRuleLogic.AND, conditions, List.of()), RiskRuntimeMode.ENFORCE);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anySatisfy(error ->
                assertThat(error.code()).isEqualTo(RiskValidationErrorCode.MAX_CONDITIONS_EXCEEDED));
    }

    /**
     * 执行 rejectsNumericOperatorWithStringLiteral 相关的风控处理逻辑。
     */
    @Test
    void rejectsNumericOperatorWithStringLiteral() {
        RiskRuleValidationResult result = validator.validate(parser.parse("""
                {
                  "logic": "AND",
                  "conditions": [
                    {
                      "left": { "type": "FEATURE", "key": "risk.score" },
                      "op": ">=",
                      "right": { "type": "LITERAL", "value": "high" }
                    }
                  ],
                  "groups": []
                }
                """), RiskRuntimeMode.ENFORCE);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anySatisfy(error -> {
            assertThat(error.path()).isEqualTo("$.conditions[0].right.value");
            assertThat(error.code()).isEqualTo(RiskValidationErrorCode.TYPE_MISMATCH);
        });
    }
}
