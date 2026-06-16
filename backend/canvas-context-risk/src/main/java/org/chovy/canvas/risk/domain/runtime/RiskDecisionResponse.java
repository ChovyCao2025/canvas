package org.chovy.canvas.risk.domain.runtime;

import org.chovy.canvas.risk.domain.dsl.RiskRuntimeMode;

import java.util.List;
import java.util.Objects;

/**
 * 对外风控决策结果，包含审计标签、命中规则引用和追踪可用性。
 *
 * @param requestId 请求幂等编号
 * @param decisionRunId 决策运行编号
 * @param sceneKey 场景业务键
 * @param strategyKey 策略业务键
 * @param strategyVersion 策略版本号
 * @param mode 运行模式
 * @param action 最终决策动作
 * @param score 最终风险分
 * @param riskBand 风险带
 * @param reasons 决策原因列表
 * @param matchedRules 命中规则引用列表
 * @param labels 决策标签列表
 * @param missingFeatures 缺失特征列表
 * @param latencyMs 决策耗时
 * @param traceAvailable 是否可追踪
 */
public final class RiskDecisionResponse {

    /**
     * RiskDecisionResponse 的 requestId 字段。
     */
    private final String requestId;


    /**
     * RiskDecisionResponse 的 decisionRunId 字段。
     */
    private final String decisionRunId;


    /**
     * RiskDecisionResponse 的 sceneKey 字段。
     */
    private final String sceneKey;


    /**
     * RiskDecisionResponse 的 strategyKey 字段。
     */
    private final String strategyKey;


    /**
     * RiskDecisionResponse 的 strategyVersion 字段。
     */
    private final int strategyVersion;


    /**
     * RiskDecisionResponse 的 mode 字段。
     */
    private final RiskRuntimeMode mode;


    /**
     * RiskDecisionResponse 的 action 字段。
     */
    private final RiskDecisionAction action;


    /**
     * RiskDecisionResponse 的 score 字段。
     */
    private final int score;


    /**
     * RiskDecisionResponse 的 riskBand 字段。
     */
    private final RiskBand riskBand;


    /**
     * RiskDecisionResponse 的 reasons 字段。
     */
    private final List<String> reasons;


    /**
     * RiskDecisionResponse 的 matchedRules 字段。
     */
    private final List<String> matchedRules;


    /**
     * RiskDecisionResponse 的 labels 字段。
     */
    private final List<String> labels;


    /**
     * RiskDecisionResponse 的 missingFeatures 字段。
     */
    private final List<String> missingFeatures;


    /**
     * RiskDecisionResponse 的 latencyMs 字段。
     */
    private final int latencyMs;


    /**
     * RiskDecisionResponse 的 traceAvailable 字段。
     */
    private final boolean traceAvailable;


    /**
     * 创建 RiskDecisionResponse。
     *
     * @param requestId RiskDecisionResponse 的 requestId 字段
     * @param decisionRunId RiskDecisionResponse 的 decisionRunId 字段
     * @param sceneKey RiskDecisionResponse 的 sceneKey 字段
     * @param strategyKey RiskDecisionResponse 的 strategyKey 字段
     * @param strategyVersion RiskDecisionResponse 的 strategyVersion 字段
     * @param mode RiskDecisionResponse 的 mode 字段
     * @param action RiskDecisionResponse 的 action 字段
     * @param score RiskDecisionResponse 的 score 字段
     * @param riskBand RiskDecisionResponse 的 riskBand 字段
     * @param reasons RiskDecisionResponse 的 reasons 字段
     * @param matchedRules RiskDecisionResponse 的 matchedRules 字段
     * @param labels RiskDecisionResponse 的 labels 字段
     * @param missingFeatures RiskDecisionResponse 的 missingFeatures 字段
     * @param latencyMs RiskDecisionResponse 的 latencyMs 字段
     * @param traceAvailable RiskDecisionResponse 的 traceAvailable 字段
     */
    public RiskDecisionResponse(String requestId, String decisionRunId, String sceneKey, String strategyKey, int strategyVersion, RiskRuntimeMode mode, RiskDecisionAction action, int score, RiskBand riskBand, List<String> reasons, List<String> matchedRules, List<String> labels, List<String> missingFeatures, int latencyMs, boolean traceAvailable) {
        this.requestId = requestId;
        this.decisionRunId = decisionRunId;
        this.sceneKey = sceneKey;
        this.strategyKey = strategyKey;
        this.strategyVersion = strategyVersion;
        this.mode = mode;
        this.action = action;
        this.score = score;
        this.riskBand = riskBand;
        this.reasons = reasons;
        this.matchedRules = matchedRules;
        this.labels = labels;
        this.missingFeatures = missingFeatures;
        this.latencyMs = latencyMs;
        this.traceAvailable = traceAvailable;
    }

    /**
     * 返回 RiskDecisionResponse 的 requestId 字段。
     *
     * @return requestId 字段值
     */
    public String requestId() {
        return requestId;
    }

    /**
     * 返回 RiskDecisionResponse 的 decisionRunId 字段。
     *
     * @return decisionRunId 字段值
     */
    public String decisionRunId() {
        return decisionRunId;
    }

    /**
     * 返回 RiskDecisionResponse 的 sceneKey 字段。
     *
     * @return sceneKey 字段值
     */
    public String sceneKey() {
        return sceneKey;
    }

    /**
     * 返回 RiskDecisionResponse 的 strategyKey 字段。
     *
     * @return strategyKey 字段值
     */
    public String strategyKey() {
        return strategyKey;
    }

    /**
     * 返回 RiskDecisionResponse 的 strategyVersion 字段。
     *
     * @return strategyVersion 字段值
     */
    public int strategyVersion() {
        return strategyVersion;
    }

    /**
     * 返回 RiskDecisionResponse 的 mode 字段。
     *
     * @return mode 字段值
     */
    public RiskRuntimeMode mode() {
        return mode;
    }

    /**
     * 返回 RiskDecisionResponse 的 action 字段。
     *
     * @return action 字段值
     */
    public RiskDecisionAction action() {
        return action;
    }

    /**
     * 返回 RiskDecisionResponse 的 score 字段。
     *
     * @return score 字段值
     */
    public int score() {
        return score;
    }

    /**
     * 返回 RiskDecisionResponse 的 riskBand 字段。
     *
     * @return riskBand 字段值
     */
    public RiskBand riskBand() {
        return riskBand;
    }

    /**
     * 返回 RiskDecisionResponse 的 reasons 字段。
     *
     * @return reasons 字段值
     */
    public List<String> reasons() {
        return reasons;
    }

    /**
     * 返回 RiskDecisionResponse 的 matchedRules 字段。
     *
     * @return matchedRules 字段值
     */
    public List<String> matchedRules() {
        return matchedRules;
    }

    /**
     * 返回 RiskDecisionResponse 的 labels 字段。
     *
     * @return labels 字段值
     */
    public List<String> labels() {
        return labels;
    }

    /**
     * 返回 RiskDecisionResponse 的 missingFeatures 字段。
     *
     * @return missingFeatures 字段值
     */
    public List<String> missingFeatures() {
        return missingFeatures;
    }

    /**
     * 返回 RiskDecisionResponse 的 latencyMs 字段。
     *
     * @return latencyMs 字段值
     */
    public int latencyMs() {
        return latencyMs;
    }

    /**
     * 返回 RiskDecisionResponse 的 traceAvailable 字段。
     *
     * @return traceAvailable 字段值
     */
    public boolean traceAvailable() {
        return traceAvailable;
    }

    /**
     * 比较当前 RiskDecisionResponse 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskDecisionResponse other)) {
            return false;
        }
        return Objects.equals(requestId, other.requestId)
                && Objects.equals(decisionRunId, other.decisionRunId)
                && Objects.equals(sceneKey, other.sceneKey)
                && Objects.equals(strategyKey, other.strategyKey)
                && strategyVersion == other.strategyVersion
                && Objects.equals(mode, other.mode)
                && Objects.equals(action, other.action)
                && score == other.score
                && Objects.equals(riskBand, other.riskBand)
                && Objects.equals(reasons, other.reasons)
                && Objects.equals(matchedRules, other.matchedRules)
                && Objects.equals(labels, other.labels)
                && Objects.equals(missingFeatures, other.missingFeatures)
                && latencyMs == other.latencyMs
                && traceAvailable == other.traceAvailable;
    }

    /**
     * 计算 RiskDecisionResponse 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(requestId, decisionRunId, sceneKey, strategyKey, strategyVersion, mode, action, score, riskBand, reasons, matchedRules, labels, missingFeatures, latencyMs, traceAvailable);
    }

    /**
     * 返回 RiskDecisionResponse 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskDecisionResponse[requestId=" + requestId + ", decisionRunId=" + decisionRunId + ", sceneKey=" + sceneKey + ", strategyKey=" + strategyKey + ", strategyVersion=" + strategyVersion + ", mode=" + mode + ", action=" + action + ", score=" + score + ", riskBand=" + riskBand + ", reasons=" + reasons + ", matchedRules=" + matchedRules + ", labels=" + labels + ", missingFeatures=" + missingFeatures + ", latencyMs=" + latencyMs + ", traceAvailable=" + traceAvailable + "]";
    }
}
