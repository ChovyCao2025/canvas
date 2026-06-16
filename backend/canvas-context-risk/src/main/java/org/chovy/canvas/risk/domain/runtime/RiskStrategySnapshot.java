package org.chovy.canvas.risk.domain.runtime;

import org.chovy.canvas.risk.domain.dsl.RiskRuntimeMode;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 持久化策略版本快照，作为编译器输入和运行时缓存身份。
 *
 * @param tenantId 租户编号
 * @param sceneKey 场景业务键
 * @param strategyKey 策略业务键
 * @param version 策略版本号
 * @param mode 运行模式
 * @param trafficPercent 流量比例
 * @param failPolicy 失败策略
 * @param latencyBudgetMs 延迟预算
 * @param groups 规则组列表
 * @param metadata 策略元数据
 */
public final class RiskStrategySnapshot {

    /**
     * RiskStrategySnapshot 的 tenantId 字段。
     */
    private final Long tenantId;


    /**
     * RiskStrategySnapshot 的 sceneKey 字段。
     */
    private final String sceneKey;


    /**
     * RiskStrategySnapshot 的 strategyKey 字段。
     */
    private final String strategyKey;


    /**
     * RiskStrategySnapshot 的 version 字段。
     */
    private final int version;


    /**
     * RiskStrategySnapshot 的 mode 字段。
     */
    private final RiskRuntimeMode mode;


    /**
     * RiskStrategySnapshot 的 trafficPercent 字段。
     */
    private final int trafficPercent;


    /**
     * RiskStrategySnapshot 的 failPolicy 字段。
     */
    private final RiskFailPolicy failPolicy;


    /**
     * RiskStrategySnapshot 的 latencyBudgetMs 字段。
     */
    private final int latencyBudgetMs;


    /**
     * RiskStrategySnapshot 的 groups 字段。
     */
    private final List<RiskStrategyRuleGroupDefinition> groups;


    /**
     * RiskStrategySnapshot 的 metadata 字段。
     */
    private final Map<String, Object> metadata;


    /**
     * 创建 RiskStrategySnapshot。
     *
     * @param tenantId RiskStrategySnapshot 的 tenantId 字段
     * @param sceneKey RiskStrategySnapshot 的 sceneKey 字段
     * @param strategyKey RiskStrategySnapshot 的 strategyKey 字段
     * @param version RiskStrategySnapshot 的 version 字段
     * @param mode RiskStrategySnapshot 的 mode 字段
     * @param trafficPercent RiskStrategySnapshot 的 trafficPercent 字段
     * @param failPolicy RiskStrategySnapshot 的 failPolicy 字段
     * @param latencyBudgetMs RiskStrategySnapshot 的 latencyBudgetMs 字段
     * @param groups RiskStrategySnapshot 的 groups 字段
     * @param metadata RiskStrategySnapshot 的 metadata 字段
     */
    public RiskStrategySnapshot(Long tenantId, String sceneKey, String strategyKey, int version, RiskRuntimeMode mode, int trafficPercent, RiskFailPolicy failPolicy, int latencyBudgetMs, List<RiskStrategyRuleGroupDefinition> groups, Map<String, Object> metadata) {
        groups = groups == null ? List.of() : List.copyOf(groups);
                metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        this.tenantId = tenantId;
        this.sceneKey = sceneKey;
        this.strategyKey = strategyKey;
        this.version = version;
        this.mode = mode;
        this.trafficPercent = trafficPercent;
        this.failPolicy = failPolicy;
        this.latencyBudgetMs = latencyBudgetMs;
        this.groups = groups;
        this.metadata = metadata;
    }

    /**
     * 返回 RiskStrategySnapshot 的 tenantId 字段。
     *
     * @return tenantId 字段值
     */
    public Long tenantId() {
        return tenantId;
    }

    /**
     * 返回 RiskStrategySnapshot 的 sceneKey 字段。
     *
     * @return sceneKey 字段值
     */
    public String sceneKey() {
        return sceneKey;
    }

    /**
     * 返回 RiskStrategySnapshot 的 strategyKey 字段。
     *
     * @return strategyKey 字段值
     */
    public String strategyKey() {
        return strategyKey;
    }

    /**
     * 返回 RiskStrategySnapshot 的 version 字段。
     *
     * @return version 字段值
     */
    public int version() {
        return version;
    }

    /**
     * 返回 RiskStrategySnapshot 的 mode 字段。
     *
     * @return mode 字段值
     */
    public RiskRuntimeMode mode() {
        return mode;
    }

    /**
     * 返回 RiskStrategySnapshot 的 trafficPercent 字段。
     *
     * @return trafficPercent 字段值
     */
    public int trafficPercent() {
        return trafficPercent;
    }

    /**
     * 返回 RiskStrategySnapshot 的 failPolicy 字段。
     *
     * @return failPolicy 字段值
     */
    public RiskFailPolicy failPolicy() {
        return failPolicy;
    }

    /**
     * 返回 RiskStrategySnapshot 的 latencyBudgetMs 字段。
     *
     * @return latencyBudgetMs 字段值
     */
    public int latencyBudgetMs() {
        return latencyBudgetMs;
    }

    /**
     * 返回 RiskStrategySnapshot 的 groups 字段。
     *
     * @return groups 字段值
     */
    public List<RiskStrategyRuleGroupDefinition> groups() {
        return groups;
    }

    /**
     * 返回 RiskStrategySnapshot 的 metadata 字段。
     *
     * @return metadata 字段值
     */
    public Map<String, Object> metadata() {
        return metadata;
    }

    /**
     * 比较当前 RiskStrategySnapshot 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskStrategySnapshot other)) {
            return false;
        }
        return Objects.equals(tenantId, other.tenantId)
                && Objects.equals(sceneKey, other.sceneKey)
                && Objects.equals(strategyKey, other.strategyKey)
                && version == other.version
                && Objects.equals(mode, other.mode)
                && trafficPercent == other.trafficPercent
                && Objects.equals(failPolicy, other.failPolicy)
                && latencyBudgetMs == other.latencyBudgetMs
                && Objects.equals(groups, other.groups)
                && Objects.equals(metadata, other.metadata);
    }

    /**
     * 计算 RiskStrategySnapshot 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(tenantId, sceneKey, strategyKey, version, mode, trafficPercent, failPolicy, latencyBudgetMs, groups, metadata);
    }

    /**
     * 返回 RiskStrategySnapshot 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskStrategySnapshot[tenantId=" + tenantId + ", sceneKey=" + sceneKey + ", strategyKey=" + strategyKey + ", version=" + version + ", mode=" + mode + ", trafficPercent=" + trafficPercent + ", failPolicy=" + failPolicy + ", latencyBudgetMs=" + latencyBudgetMs + ", groups=" + groups + ", metadata=" + metadata + "]";
    }

    /**
         * 返回运行时缓存键。
         */
        public String cacheKey() {
            return tenantId + ":" + sceneKey + ":" + strategyKey + ":" + version;
        }
}
