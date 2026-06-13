package org.chovy.canvas.risk.domain.dsl;

import org.chovy.canvas.risk.adapter.external.JacksonRiskRuleJsonCodec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskRuleParserTest {

    private final RiskRuleParser parser = new RiskRuleParser(new JacksonRiskRuleJsonCodec());

    @Test
    void parsesNestedAndOrRuleGroup() {
        RiskRuleGroupNode node = parser.parse("""
                {
                  "logic": "AND",
                  "conditions": [
                    {
                      "left": { "type": "FEATURE", "key": "risk.score" },
                      "op": ">=",
                      "right": { "type": "LITERAL", "value": 85 }
                    },
                    {
                      "left": { "type": "EVENT", "path": "amount" },
                      "op": "<",
                      "right": { "type": "LITERAL", "value": 500 }
                    }
                  ],
                  "groups": [
                    {
                      "logic": "OR",
                      "conditions": [
                        {
                          "left": { "type": "FEATURE", "key": "device.change_card_1d" },
                          "op": ">",
                          "right": { "type": "LITERAL", "value": 2 }
                        }
                      ],
                      "groups": []
                    }
                  ]
                }
                """);

        assertThat(node.logic()).isEqualTo(RiskRuleLogic.AND);
        assertThat(node.conditions()).hasSize(2);
        assertThat(node.groups()).hasSize(1);
        assertThat(node.groups().getFirst().logic()).isEqualTo(RiskRuleLogic.OR);
        assertThat(node.conditions().getFirst().left()).isEqualTo(RiskRuleOperand.feature("risk.score"));
        assertThat(node.conditions().getFirst().op()).isEqualTo(RiskRuleOperator.GTE);
        assertThat(node.conditions().get(1).left()).isEqualTo(RiskRuleOperand.event("amount"));
    }

    @Test
    void parsesAllSupportedOperandTypes() {
        RiskRuleGroupNode node = parser.parse("""
                {
                  "logic": "AND",
                  "conditions": [
                    {
                      "left": { "type": "SUBJECT", "path": "userId" },
                      "op": "IN",
                      "right": { "type": "LIST", "key": "blacklist.user" }
                    },
                    {
                      "left": { "type": "CONTEXT", "path": "caller" },
                      "op": "==",
                      "right": { "type": "LITERAL", "value": "CANVAS_NODE" }
                    }
                  ],
                  "groups": []
                }
                """);

        assertThat(node.conditions().getFirst().left()).isEqualTo(RiskRuleOperand.subject("userId"));
        assertThat(node.conditions().getFirst().right()).isEqualTo(RiskRuleOperand.list("blacklist.user"));
        assertThat(node.conditions().get(1).left()).isEqualTo(RiskRuleOperand.context("caller"));
        assertThat(node.conditions().get(1).right()).isEqualTo(RiskRuleOperand.literal("CANVAS_NODE"));
    }

    @Test
    void rejectsInvalidJson() {
        assertThatExceptionOfType(RiskRuleParseException.class)
                .isThrownBy(() -> parser.parse("{not-json"))
                .satisfies(error -> {
                    assertThat(error.code()).isEqualTo(RiskValidationErrorCode.INVALID_JSON);
                    assertThat(error.path()).isEqualTo("$");
                });
    }

    @Test
    void rejectsUnknownOperator() {
        assertThatExceptionOfType(RiskRuleParseException.class)
                .isThrownBy(() -> parser.parse("""
                        {
                          "logic": "AND",
                          "conditions": [
                            {
                              "left": { "type": "FEATURE", "key": "risk.score" },
                              "op": "EVAL",
                              "right": { "type": "LITERAL", "value": 85 }
                            }
                          ],
                          "groups": []
                        }
                        """))
                .satisfies(error -> {
                    assertThat(error.code()).isEqualTo(RiskValidationErrorCode.UNKNOWN_OPERATOR);
                    assertThat(error.path()).isEqualTo("$.conditions[0].op");
                });
    }

    @Test
    void rejectsUnsafeScriptOperand() {
        assertThatThrownBy(() -> parser.parse("""
                {
                  "logic": "AND",
                  "conditions": [
                    {
                      "left": { "type": "SCRIPT", "body": "java.lang.Runtime.getRuntime().exec('x')" },
                      "op": "==",
                      "right": { "type": "LITERAL", "value": true }
                    }
                  ],
                  "groups": []
                }
                """))
                .isInstanceOf(RiskRuleParseException.class)
                .hasMessageContaining("UNKNOWN_OPERAND_TYPE");
    }
}
