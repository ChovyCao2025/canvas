package org.chovy.canvas.web.risk.dto;

/**
 * 风控仿真启动请求 DTO。
 *
 * @param tenantId 请求体租户提示，实际租户以认证上下文为准
 * @param sceneKey 场景业务键
 * @param strategyKey 策略业务键
 * @param version 兼容旧客户端的候选版本号
 * @param candidateVersion 候选版本号
 * @param sampleLimit 样本数量上限
 */
public final class RiskSimulationStartRequest {

    /**
     * 租户标识。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("tenantId")
    private final Long tenantId;

    /**
     * 场景业务键。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("sceneKey")
    private final String sceneKey;

    /**
     * 策略业务键。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("strategyKey")
    private final String strategyKey;

    /**
     * 版本号。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("version")
    private final Integer version;

    /**
     * 候选版本号。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("candidateVersion")
    private final Integer candidateVersion;

    /**
     * 样本数量上限。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("sampleLimit")
    private final Integer sampleLimit;

    /**
     * 创建 RiskSimulationStartRequest 实例。
     *
     * @param tenantId 租户标识
     * @param sceneKey 场景业务键
     * @param strategyKey 策略业务键
     * @param version 版本号
     * @param candidateVersion 候选版本号
     * @param sampleLimit 样本数量上限
     */
    @com.fasterxml.jackson.annotation.JsonCreator
    public RiskSimulationStartRequest(@com.fasterxml.jackson.annotation.JsonProperty("tenantId") Long tenantId, @com.fasterxml.jackson.annotation.JsonProperty("sceneKey") String sceneKey, @com.fasterxml.jackson.annotation.JsonProperty("strategyKey") String strategyKey, @com.fasterxml.jackson.annotation.JsonProperty("version") Integer version, @com.fasterxml.jackson.annotation.JsonProperty("candidateVersion") Integer candidateVersion, @com.fasterxml.jackson.annotation.JsonProperty("sampleLimit") Integer sampleLimit) {
        this.tenantId = tenantId;
        this.sceneKey = sceneKey;
        this.strategyKey = strategyKey;
        this.version = version;
        this.candidateVersion = candidateVersion;
        this.sampleLimit = sampleLimit;
    }

    /**
     * 返回租户标识。
     *
     * @return 租户标识
     */
    public Long tenantId() {
        return tenantId;
    }

    /**
     * 返回场景业务键。
     *
     * @return 场景业务键
     */
    public String sceneKey() {
        return sceneKey;
    }

    /**
     * 返回策略业务键。
     *
     * @return 策略业务键
     */
    public String strategyKey() {
        return strategyKey;
    }

    /**
     * 返回版本号。
     *
     * @return 版本号
     */
    public Integer version() {
        return version;
    }

    /**
     * 返回候选版本号。
     *
     * @return 候选版本号
     */
    public Integer candidateVersion() {
        return candidateVersion;
    }

    /**
     * 返回样本数量上限。
     *
     * @return 样本数量上限
     */
    public Integer sampleLimit() {
        return sampleLimit;
    }

    /**
     * 判断两个 RiskSimulationStartRequest 实例是否包含相同字段值。
     *
     * @param o 待比较对象
     * @return 字段值全部一致时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskSimulationStartRequest that)) {
            return false;
        }
        return java.util.Objects.equals(tenantId, that.tenantId) && java.util.Objects.equals(sceneKey, that.sceneKey) && java.util.Objects.equals(strategyKey, that.strategyKey) && java.util.Objects.equals(version, that.version) && java.util.Objects.equals(candidateVersion, that.candidateVersion) && java.util.Objects.equals(sampleLimit, that.sampleLimit);
    }

    /**
     * 根据全部字段生成哈希值。
     *
     * @return 字段哈希值
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(tenantId, sceneKey, strategyKey, version, candidateVersion, sampleLimit);
    }

    /**
     * 返回与原记录形态一致的调试字符串。
     *
     * @return 字段调试字符串
     */
    @Override
    public String toString() {
        return "RiskSimulationStartRequest[" + "tenantId=" + tenantId + ", " + "sceneKey=" + sceneKey + ", " + "strategyKey=" + strategyKey + ", " + "version=" + version + ", " + "candidateVersion=" + candidateVersion + ", " + "sampleLimit=" + sampleLimit + "]";
    }
}
