package org.chovy.canvas.risk.api;

import java.util.Objects;

/**
 * 定义 RiskSimulationCommand 的风控模块职责和数据契约。
 */
public final class RiskSimulationCommand {

    /**
     * RiskSimulationCommand 的 tenantId 字段。
     */
    private final Long tenantId;


    /**
     * RiskSimulationCommand 的 sceneKey 字段。
     */
    private final String sceneKey;


    /**
     * RiskSimulationCommand 的 strategyKey 字段。
     */
    private final String strategyKey;


    /**
     * RiskSimulationCommand 的 baselineVersion 字段。
     */
    private final int baselineVersion;


    /**
     * RiskSimulationCommand 的 candidateVersion 字段。
     */
    private final int candidateVersion;


    /**
     * RiskSimulationCommand 的 sampleLimit 字段。
     */
    private final int sampleLimit;


    /**
     * 创建 RiskSimulationCommand。
     *
     * @param tenantId RiskSimulationCommand 的 tenantId 字段
     * @param sceneKey RiskSimulationCommand 的 sceneKey 字段
     * @param strategyKey RiskSimulationCommand 的 strategyKey 字段
     * @param baselineVersion RiskSimulationCommand 的 baselineVersion 字段
     * @param candidateVersion RiskSimulationCommand 的 candidateVersion 字段
     * @param sampleLimit RiskSimulationCommand 的 sampleLimit 字段
     */
    public RiskSimulationCommand(Long tenantId, String sceneKey, String strategyKey, int baselineVersion, int candidateVersion, int sampleLimit) {
        this.tenantId = tenantId;
        this.sceneKey = sceneKey;
        this.strategyKey = strategyKey;
        this.baselineVersion = baselineVersion;
        this.candidateVersion = candidateVersion;
        this.sampleLimit = sampleLimit;
    }

    /**
     * 返回 RiskSimulationCommand 的 tenantId 字段。
     *
     * @return tenantId 字段值
     */
    public Long tenantId() {
        return tenantId;
    }

    /**
     * 返回 RiskSimulationCommand 的 sceneKey 字段。
     *
     * @return sceneKey 字段值
     */
    public String sceneKey() {
        return sceneKey;
    }

    /**
     * 返回 RiskSimulationCommand 的 strategyKey 字段。
     *
     * @return strategyKey 字段值
     */
    public String strategyKey() {
        return strategyKey;
    }

    /**
     * 返回 RiskSimulationCommand 的 baselineVersion 字段。
     *
     * @return baselineVersion 字段值
     */
    public int baselineVersion() {
        return baselineVersion;
    }

    /**
     * 返回 RiskSimulationCommand 的 candidateVersion 字段。
     *
     * @return candidateVersion 字段值
     */
    public int candidateVersion() {
        return candidateVersion;
    }

    /**
     * 返回 RiskSimulationCommand 的 sampleLimit 字段。
     *
     * @return sampleLimit 字段值
     */
    public int sampleLimit() {
        return sampleLimit;
    }

    /**
     * 比较当前 RiskSimulationCommand 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskSimulationCommand other)) {
            return false;
        }
        return Objects.equals(tenantId, other.tenantId)
                && Objects.equals(sceneKey, other.sceneKey)
                && Objects.equals(strategyKey, other.strategyKey)
                && baselineVersion == other.baselineVersion
                && candidateVersion == other.candidateVersion
                && sampleLimit == other.sampleLimit;
    }

    /**
     * 计算 RiskSimulationCommand 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(tenantId, sceneKey, strategyKey, baselineVersion, candidateVersion, sampleLimit);
    }

    /**
     * 返回 RiskSimulationCommand 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskSimulationCommand[tenantId=" + tenantId + ", sceneKey=" + sceneKey + ", strategyKey=" + strategyKey + ", baselineVersion=" + baselineVersion + ", candidateVersion=" + candidateVersion + ", sampleLimit=" + sampleLimit + "]";
    }
}
