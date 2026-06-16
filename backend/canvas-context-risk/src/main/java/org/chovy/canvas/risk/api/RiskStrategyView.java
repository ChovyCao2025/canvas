package org.chovy.canvas.risk.api;

import java.util.Objects;

/**
 * 定义 RiskStrategyView 的风控模块职责和数据契约。
 */
public final class RiskStrategyView {

    /**
     * RiskStrategyView 的 tenantId 字段。
     */
    private final Long tenantId;


    /**
     * RiskStrategyView 的 sceneKey 字段。
     */
    private final String sceneKey;


    /**
     * RiskStrategyView 的 strategyKey 字段。
     */
    private final String strategyKey;


    /**
     * RiskStrategyView 的 name 字段。
     */
    private final String name;


    /**
     * RiskStrategyView 的 status 字段。
     */
    private final String status;


    /**
     * RiskStrategyView 的 activeVersion 字段。
     */
    private final Integer activeVersion;


    /**
     * RiskStrategyView 的 draftVersion 字段。
     */
    private final Integer draftVersion;


    /**
     * RiskStrategyView 的 riskLevel 字段。
     */
    private final String riskLevel;


    /**
     * RiskStrategyView 的 owner 字段。
     */
    private final String owner;


    /**
     * 创建 RiskStrategyView。
     *
     * @param tenantId RiskStrategyView 的 tenantId 字段
     * @param sceneKey RiskStrategyView 的 sceneKey 字段
     * @param strategyKey RiskStrategyView 的 strategyKey 字段
     * @param name RiskStrategyView 的 name 字段
     * @param status RiskStrategyView 的 status 字段
     * @param activeVersion RiskStrategyView 的 activeVersion 字段
     * @param draftVersion RiskStrategyView 的 draftVersion 字段
     * @param riskLevel RiskStrategyView 的 riskLevel 字段
     * @param owner RiskStrategyView 的 owner 字段
     */
    public RiskStrategyView(Long tenantId, String sceneKey, String strategyKey, String name, String status, Integer activeVersion, Integer draftVersion, String riskLevel, String owner) {
        this.tenantId = tenantId;
        this.sceneKey = sceneKey;
        this.strategyKey = strategyKey;
        this.name = name;
        this.status = status;
        this.activeVersion = activeVersion;
        this.draftVersion = draftVersion;
        this.riskLevel = riskLevel;
        this.owner = owner;
    }

    /**
     * 返回 RiskStrategyView 的 tenantId 字段。
     *
     * @return tenantId 字段值
     */
    public Long tenantId() {
        return tenantId;
    }

    /**
     * 返回 RiskStrategyView 的 sceneKey 字段。
     *
     * @return sceneKey 字段值
     */
    public String sceneKey() {
        return sceneKey;
    }

    /**
     * 返回 RiskStrategyView 的 strategyKey 字段。
     *
     * @return strategyKey 字段值
     */
    public String strategyKey() {
        return strategyKey;
    }

    /**
     * 返回 RiskStrategyView 的 name 字段。
     *
     * @return name 字段值
     */
    public String name() {
        return name;
    }

    /**
     * 返回 RiskStrategyView 的 status 字段。
     *
     * @return status 字段值
     */
    public String status() {
        return status;
    }

    /**
     * 返回 RiskStrategyView 的 activeVersion 字段。
     *
     * @return activeVersion 字段值
     */
    public Integer activeVersion() {
        return activeVersion;
    }

    /**
     * 返回 RiskStrategyView 的 draftVersion 字段。
     *
     * @return draftVersion 字段值
     */
    public Integer draftVersion() {
        return draftVersion;
    }

    /**
     * 返回 RiskStrategyView 的 riskLevel 字段。
     *
     * @return riskLevel 字段值
     */
    public String riskLevel() {
        return riskLevel;
    }

    /**
     * 返回 RiskStrategyView 的 owner 字段。
     *
     * @return owner 字段值
     */
    public String owner() {
        return owner;
    }

    /**
     * 比较当前 RiskStrategyView 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskStrategyView other)) {
            return false;
        }
        return Objects.equals(tenantId, other.tenantId)
                && Objects.equals(sceneKey, other.sceneKey)
                && Objects.equals(strategyKey, other.strategyKey)
                && Objects.equals(name, other.name)
                && Objects.equals(status, other.status)
                && Objects.equals(activeVersion, other.activeVersion)
                && Objects.equals(draftVersion, other.draftVersion)
                && Objects.equals(riskLevel, other.riskLevel)
                && Objects.equals(owner, other.owner);
    }

    /**
     * 计算 RiskStrategyView 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(tenantId, sceneKey, strategyKey, name, status, activeVersion, draftVersion, riskLevel, owner);
    }

    /**
     * 返回 RiskStrategyView 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskStrategyView[tenantId=" + tenantId + ", sceneKey=" + sceneKey + ", strategyKey=" + strategyKey + ", name=" + name + ", status=" + status + ", activeVersion=" + activeVersion + ", draftVersion=" + draftVersion + ", riskLevel=" + riskLevel + ", owner=" + owner + "]";
    }
}
