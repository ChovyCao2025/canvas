package org.chovy.canvas.engine.rule;

import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AudienceDefinitionRuleValidator {

    private static final Set<String> SUPPORTED_ENGINES = Set.of("AVIATOR", "QL");

    private final RuleParser ruleParser;
    private final RuleValidator ruleValidator;

    public AudienceDefinitionRuleValidator(RuleParser ruleParser, RuleValidator ruleValidator) {
        this.ruleParser = ruleParser;
        this.ruleValidator = ruleValidator;
    }

    public void validateForSave(AudienceDefinitionDO definition) {
        if (definition == null) {
            throw new RuleValidationException("人群定义不能为空");
        }
        String engineType = definition.getEngineType();
        if (engineType == null || !SUPPORTED_ENGINES.contains(engineType)) {
            throw new RuleValidationException("未知规则引擎: " + engineType);
        }
        try {
            RuleGroup rule = ruleParser.parseAudienceJson(definition.getRuleJson());
            ruleValidator.validateOrThrow(rule, RuleValidationOptions.strict("audience.ruleJson"));
        } catch (RuleValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuleValidationException("人群规则 JSON 解析失败: " + e.getMessage());
        }
    }
}
