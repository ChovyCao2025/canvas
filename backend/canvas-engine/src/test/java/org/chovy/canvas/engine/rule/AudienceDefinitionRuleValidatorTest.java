package org.chovy.canvas.engine.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AudienceDefinitionRuleValidatorTest {

    private final AudienceDefinitionRuleValidator validator = new AudienceDefinitionRuleValidator(
            new RuleParser(new ObjectMapper()),
            new RuleValidator());

    @Test
    void rejectsUnknownEngineType() {
        AudienceDefinitionDO definition = definition("MVEL", """
                {"logic":"AND","conditions":[{"field":"city","op":"=","value":"Beijing"}]}
                """);

        assertThatThrownBy(() -> validator.validateForSave(definition))
                .isInstanceOf(RuleValidationException.class)
                .hasMessageContaining("未知规则引擎");
    }

    @Test
    void rejectsEmptyAudienceRuleUnlessMatchAllIsExplicit() {
        AudienceDefinitionDO definition = definition("AVIATOR", """
                {"logic":"AND","conditions":[],"groups":[]}
                """);

        assertThatThrownBy(() -> validator.validateForSave(definition))
                .isInstanceOf(RuleValidationException.class)
                .hasMessageContaining("规则不能为空");
    }

    private static AudienceDefinitionDO definition(String engineType, String ruleJson) {
        AudienceDefinitionDO definition = new AudienceDefinitionDO();
        definition.setName("test");
        definition.setEngineType(engineType);
        definition.setRuleJson(ruleJson);
        definition.setDataSourceType("JDBC");
        definition.setDataSourceConfig("{}");
        definition.setEnabled(1);
        return definition;
    }
}
