package org.chovy.canvas.risk.api;

import java.util.List;
import java.util.Objects;

/**
 * 定义 RiskDecisionView 的风控模块职责和数据契约。
 */
public final class RiskDecisionView {

    /**
     * RiskDecisionView 的 requestId 字段。
     */
    private final String requestId;


    /**
     * RiskDecisionView 的 decisionRunId 字段。
     */
    private final String decisionRunId;


    /**
     * RiskDecisionView 的 sceneKey 字段。
     */
    private final String sceneKey;


    /**
     * RiskDecisionView 的 strategyKey 字段。
     */
    private final String strategyKey;


    /**
     * RiskDecisionView 的 strategyVersion 字段。
     */
    private final int strategyVersion;


    /**
     * RiskDecisionView 的 mode 字段。
     */
    private final String mode;


    /**
     * RiskDecisionView 的 decision 字段。
     */
    private final String decision;


    /**
     * RiskDecisionView 的 score 字段。
     */
    private final int score;


    /**
     * RiskDecisionView 的 riskBand 字段。
     */
    private final String riskBand;


    /**
     * RiskDecisionView 的 reasons 字段。
     */
    private final List<String> reasons;


    /**
     * RiskDecisionView 的 matchedRules 字段。
     */
    private final List<String> matchedRules;


    /**
     * RiskDecisionView 的 labels 字段。
     */
    private final List<String> labels;


    /**
     * RiskDecisionView 的 missingFeatures 字段。
     */
    private final List<String> missingFeatures;


    /**
     * RiskDecisionView 的 traceAvailable 字段。
     */
    private final boolean traceAvailable;


    /**
     * RiskDecisionView 的 latencyMs 字段。
     */
    private final int latencyMs;


    /**
     * 创建 RiskDecisionView。
     *
     * @param requestId RiskDecisionView 的 requestId 字段
     * @param decisionRunId RiskDecisionView 的 decisionRunId 字段
     * @param sceneKey RiskDecisionView 的 sceneKey 字段
     * @param strategyKey RiskDecisionView 的 strategyKey 字段
     * @param strategyVersion RiskDecisionView 的 strategyVersion 字段
     * @param mode RiskDecisionView 的 mode 字段
     * @param decision RiskDecisionView 的 decision 字段
     * @param score RiskDecisionView 的 score 字段
     * @param riskBand RiskDecisionView 的 riskBand 字段
     * @param reasons RiskDecisionView 的 reasons 字段
     * @param matchedRules RiskDecisionView 的 matchedRules 字段
     * @param labels RiskDecisionView 的 labels 字段
     * @param missingFeatures RiskDecisionView 的 missingFeatures 字段
     * @param traceAvailable RiskDecisionView 的 traceAvailable 字段
     * @param latencyMs RiskDecisionView 的 latencyMs 字段
     */
    public RiskDecisionView(String requestId, String decisionRunId, String sceneKey, String strategyKey, int strategyVersion, String mode, String decision, int score, String riskBand, List<String> reasons, List<String> matchedRules, List<String> labels, List<String> missingFeatures, boolean traceAvailable, int latencyMs) {
        this.requestId = requestId;
        this.decisionRunId = decisionRunId;
        this.sceneKey = sceneKey;
        this.strategyKey = strategyKey;
        this.strategyVersion = strategyVersion;
        this.mode = mode;
        this.decision = decision;
        this.score = score;
        this.riskBand = riskBand;
        this.reasons = reasons;
        this.matchedRules = matchedRules;
        this.labels = labels;
        this.missingFeatures = missingFeatures;
        this.traceAvailable = traceAvailable;
        this.latencyMs = latencyMs;
    }

    /**
     * 返回 RiskDecisionView 的 requestId 字段。
     *
     * @return requestId 字段值
     */
    public String requestId() {
        return requestId;
    }

    /**
     * 返回 RiskDecisionView 的 decisionRunId 字段。
     *
     * @return decisionRunId 字段值
     */
    public String decisionRunId() {
        return decisionRunId;
    }

    /**
     * 返回 RiskDecisionView 的 sceneKey 字段。
     *
     * @return sceneKey 字段值
     */
    public String sceneKey() {
        return sceneKey;
    }

    /**
     * 返回 RiskDecisionView 的 strategyKey 字段。
     *
     * @return strategyKey 字段值
     */
    public String strategyKey() {
        return strategyKey;
    }

    /**
     * 返回 RiskDecisionView 的 strategyVersion 字段。
     *
     * @return strategyVersion 字段值
     */
    public int strategyVersion() {
        return strategyVersion;
    }

    /**
     * 返回 RiskDecisionView 的 mode 字段。
     *
     * @return mode 字段值
     */
    public String mode() {
        return mode;
    }

    /**
     * 返回 RiskDecisionView 的 decision 字段。
     *
     * @return decision 字段值
     */
    public String decision() {
        return decision;
    }

    /**
     * 返回 RiskDecisionView 的 score 字段。
     *
     * @return score 字段值
     */
    public int score() {
        return score;
    }

    /**
     * 返回 RiskDecisionView 的 riskBand 字段。
     *
     * @return riskBand 字段值
     */
    public String riskBand() {
        return riskBand;
    }

    /**
     * 返回 RiskDecisionView 的 reasons 字段。
     *
     * @return reasons 字段值
     */
    public List<String> reasons() {
        return reasons;
    }

    /**
     * 返回 RiskDecisionView 的 matchedRules 字段。
     *
     * @return matchedRules 字段值
     */
    public List<String> matchedRules() {
        return matchedRules;
    }

    /**
     * 返回 RiskDecisionView 的 labels 字段。
     *
     * @return labels 字段值
     */
    public List<String> labels() {
        return labels;
    }

    /**
     * 返回 RiskDecisionView 的 missingFeatures 字段。
     *
     * @return missingFeatures 字段值
     */
    public List<String> missingFeatures() {
        return missingFeatures;
    }

    /**
     * 返回 RiskDecisionView 的 traceAvailable 字段。
     *
     * @return traceAvailable 字段值
     */
    public boolean traceAvailable() {
        return traceAvailable;
    }

    /**
     * 返回 RiskDecisionView 的 latencyMs 字段。
     *
     * @return latencyMs 字段值
     */
    public int latencyMs() {
        return latencyMs;
    }

    /**
     * 比较当前 RiskDecisionView 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskDecisionView other)) {
            return false;
        }
        return Objects.equals(requestId, other.requestId)
                && Objects.equals(decisionRunId, other.decisionRunId)
                && Objects.equals(sceneKey, other.sceneKey)
                && Objects.equals(strategyKey, other.strategyKey)
                && strategyVersion == other.strategyVersion
                && Objects.equals(mode, other.mode)
                && Objects.equals(decision, other.decision)
                && score == other.score
                && Objects.equals(riskBand, other.riskBand)
                && Objects.equals(reasons, other.reasons)
                && Objects.equals(matchedRules, other.matchedRules)
                && Objects.equals(labels, other.labels)
                && Objects.equals(missingFeatures, other.missingFeatures)
                && traceAvailable == other.traceAvailable
                && latencyMs == other.latencyMs;
    }

    /**
     * 计算 RiskDecisionView 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(requestId, decisionRunId, sceneKey, strategyKey, strategyVersion, mode, decision, score, riskBand, reasons, matchedRules, labels, missingFeatures, traceAvailable, latencyMs);
    }

    /**
     * 返回 RiskDecisionView 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskDecisionView[requestId=" + requestId + ", decisionRunId=" + decisionRunId + ", sceneKey=" + sceneKey + ", strategyKey=" + strategyKey + ", strategyVersion=" + strategyVersion + ", mode=" + mode + ", decision=" + decision + ", score=" + score + ", riskBand=" + riskBand + ", reasons=" + reasons + ", matchedRules=" + matchedRules + ", labels=" + labels + ", missingFeatures=" + missingFeatures + ", traceAvailable=" + traceAvailable + ", latencyMs=" + latencyMs + "]";
    }
}
