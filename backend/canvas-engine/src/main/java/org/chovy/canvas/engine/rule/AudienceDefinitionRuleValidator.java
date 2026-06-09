package org.chovy.canvas.engine.rule;

import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * AudienceDefinitionRuleValidator 参与 engine.rule 场景的画布执行引擎处理。
 */
@Component
public class AudienceDefinitionRuleValidator {

    private static final Set<String> SUPPORTED_ENGINES = Set.of("AVIATOR", "QL");

    private final RuleParser ruleParser;
    private final RuleValidator ruleValidator;

    /**
     * 创建 AudienceDefinitionRuleValidator 实例并注入 engine.rule 场景依赖。
     * @param ruleParser rule parser 参数，用于 AudienceDefinitionRuleValidator 流程中的校验、计算或对象转换。
     * @param ruleValidator rule validator 参数，用于 AudienceDefinitionRuleValidator 流程中的校验、计算或对象转换。
     */
    public AudienceDefinitionRuleValidator(RuleParser ruleParser, RuleValidator ruleValidator) {
        this.ruleParser = ruleParser;
        this.ruleValidator = ruleValidator;
    }

    /**
     * validateForSave 校验或转换 engine.rule 场景的数据。
     * @param definition definition 参数，用于 validateForSave 流程中的校验、计算或对象转换。
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuleValidationException e) {
            throw e;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new RuleValidationException("人群规则 JSON 解析失败: " + e.getMessage());
        }
    }
}
