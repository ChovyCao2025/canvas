package org.chovy.canvas.risk.api;

import java.util.Objects;

/**
 * 定义 RiskStrategyCommand 的风控模块职责和数据契约。
 */
public final class RiskStrategyCommand {

    /**
     * RiskStrategyCommand 的 sceneKey 字段。
     */
    private final String sceneKey;


    /**
     * RiskStrategyCommand 的 strategyKey 字段。
     */
    private final String strategyKey;


    /**
     * RiskStrategyCommand 的 name 字段。
     */
    private final String name;


    /**
     * RiskStrategyCommand 的 riskLevel 字段。
     */
    private final String riskLevel;


    /**
     * RiskStrategyCommand 的 definitionJson 字段。
     */
    private final String definitionJson;


    /**
     * 创建 RiskStrategyCommand。
     *
     * @param sceneKey RiskStrategyCommand 的 sceneKey 字段
     * @param strategyKey RiskStrategyCommand 的 strategyKey 字段
     * @param name RiskStrategyCommand 的 name 字段
     * @param riskLevel RiskStrategyCommand 的 riskLevel 字段
     * @param definitionJson RiskStrategyCommand 的 definitionJson 字段
     */
    public RiskStrategyCommand(String sceneKey, String strategyKey, String name, String riskLevel, String definitionJson) {
        this.sceneKey = sceneKey;
        this.strategyKey = strategyKey;
        this.name = name;
        this.riskLevel = riskLevel;
        this.definitionJson = definitionJson;
    }

    /**
     * 返回 RiskStrategyCommand 的 sceneKey 字段。
     *
     * @return sceneKey 字段值
     */
    public String sceneKey() {
        return sceneKey;
    }

    /**
     * 返回 RiskStrategyCommand 的 strategyKey 字段。
     *
     * @return strategyKey 字段值
     */
    public String strategyKey() {
        return strategyKey;
    }

    /**
     * 返回 RiskStrategyCommand 的 name 字段。
     *
     * @return name 字段值
     */
    public String name() {
        return name;
    }

    /**
     * 返回 RiskStrategyCommand 的 riskLevel 字段。
     *
     * @return riskLevel 字段值
     */
    public String riskLevel() {
        return riskLevel;
    }

    /**
     * 返回 RiskStrategyCommand 的 definitionJson 字段。
     *
     * @return definitionJson 字段值
     */
    public String definitionJson() {
        return definitionJson;
    }

    /**
     * 比较当前 RiskStrategyCommand 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskStrategyCommand other)) {
            return false;
        }
        return Objects.equals(sceneKey, other.sceneKey)
                && Objects.equals(strategyKey, other.strategyKey)
                && Objects.equals(name, other.name)
                && Objects.equals(riskLevel, other.riskLevel)
                && Objects.equals(definitionJson, other.definitionJson);
    }

    /**
     * 计算 RiskStrategyCommand 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(sceneKey, strategyKey, name, riskLevel, definitionJson);
    }

    /**
     * 返回 RiskStrategyCommand 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskStrategyCommand[sceneKey=" + sceneKey + ", strategyKey=" + strategyKey + ", name=" + name + ", riskLevel=" + riskLevel + ", definitionJson=" + definitionJson + "]";
    }

    /**
     * 执行 withDefinitionJson 相关的风控处理逻辑。
     */
    public RiskStrategyCommand withDefinitionJson(String newDefinitionJson) {
            return new RiskStrategyCommand(sceneKey, strategyKey, name, riskLevel, newDefinitionJson);
        }
}
