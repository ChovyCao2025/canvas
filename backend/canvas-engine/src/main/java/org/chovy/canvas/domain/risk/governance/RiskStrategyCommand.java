package org.chovy.canvas.domain.risk.governance;

/**
 * 风控策略草稿命令。
 *
 * @param sceneKey 场景业务键
 * @param strategyKey 策略业务键
 * @param name 策略名称
 * @param riskLevel 风险等级
 * @param definitionJson 策略定义 JSON
 */
public record RiskStrategyCommand(
        String sceneKey,
        String strategyKey,
        String name,
        String riskLevel,
        String definitionJson
) {

    /**
     * 返回替换策略定义 JSON 后的命令副本。
     */
    public RiskStrategyCommand withDefinitionJson(String newDefinitionJson) {
        return new RiskStrategyCommand(sceneKey, strategyKey, name, riskLevel, newDefinitionJson);
    }
}
