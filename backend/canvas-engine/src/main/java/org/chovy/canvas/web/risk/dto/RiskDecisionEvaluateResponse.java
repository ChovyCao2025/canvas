package org.chovy.canvas.web.risk.dto;

import java.util.List;

/**
 * 风控在线评估响应 DTO。
 *
 * @param requestId 请求幂等编号
 * @param decisionRunId 决策运行编号
 * @param sceneKey 场景业务键
 * @param strategyKey 策略业务键
 * @param strategyVersion 策略版本号
 * @param mode 运行模式
 * @param decision 决策动作
 * @param score 风险分
 * @param riskBand 风险带
 * @param reasons 原因列表
 * @param matchedRules 命中规则列表
 * @param labels 标签列表
 * @param missingFeatures 缺失特征列表
 * @param traceAvailable 是否可追踪
 * @param latencyMs 决策耗时
 */
public final class RiskDecisionEvaluateResponse {

    /**
     * 请求幂等标识。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("requestId")
    private final String requestId;

    /**
     * decisionRunId 字段值。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("decisionRunId")
    private final String decisionRunId;

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
     * strategyVersion 字段值。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("strategyVersion")
    private final int strategyVersion;

    /**
     * mode 字段值。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("mode")
    private final String mode;

    /**
     * decision 字段值。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("decision")
    private final String decision;

    /**
     * score 字段值。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("score")
    private final int score;

    /**
     * riskBand 字段值。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("riskBand")
    private final String riskBand;

    /**
     * reasons 字段值。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("reasons")
    private final List<String> reasons;

    /**
     * matchedRules 字段值。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("matchedRules")
    private final List<String> matchedRules;

    /**
     * labels 字段值。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("labels")
    private final List<String> labels;

    /**
     * missingFeatures 字段值。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("missingFeatures")
    private final List<String> missingFeatures;

    /**
     * traceAvailable 字段值。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("traceAvailable")
    private final boolean traceAvailable;

    /**
     * latencyMs 字段值。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("latencyMs")
    private final int latencyMs;

    /**
     * 创建 RiskDecisionEvaluateResponse 实例。
     *
     * @param requestId 请求幂等标识
     * @param decisionRunId decisionRunId 字段值
     * @param sceneKey 场景业务键
     * @param strategyKey 策略业务键
     * @param strategyVersion strategyVersion 字段值
     * @param mode mode 字段值
     * @param decision decision 字段值
     * @param score score 字段值
     * @param riskBand riskBand 字段值
     * @param reasons reasons 字段值
     * @param matchedRules matchedRules 字段值
     * @param labels labels 字段值
     * @param missingFeatures missingFeatures 字段值
     * @param traceAvailable traceAvailable 字段值
     * @param latencyMs latencyMs 字段值
     */
    @com.fasterxml.jackson.annotation.JsonCreator
    public RiskDecisionEvaluateResponse(@com.fasterxml.jackson.annotation.JsonProperty("requestId") String requestId, @com.fasterxml.jackson.annotation.JsonProperty("decisionRunId") String decisionRunId, @com.fasterxml.jackson.annotation.JsonProperty("sceneKey") String sceneKey, @com.fasterxml.jackson.annotation.JsonProperty("strategyKey") String strategyKey, @com.fasterxml.jackson.annotation.JsonProperty("strategyVersion") int strategyVersion, @com.fasterxml.jackson.annotation.JsonProperty("mode") String mode, @com.fasterxml.jackson.annotation.JsonProperty("decision") String decision, @com.fasterxml.jackson.annotation.JsonProperty("score") int score, @com.fasterxml.jackson.annotation.JsonProperty("riskBand") String riskBand, @com.fasterxml.jackson.annotation.JsonProperty("reasons") List<String> reasons, @com.fasterxml.jackson.annotation.JsonProperty("matchedRules") List<String> matchedRules, @com.fasterxml.jackson.annotation.JsonProperty("labels") List<String> labels, @com.fasterxml.jackson.annotation.JsonProperty("missingFeatures") List<String> missingFeatures, @com.fasterxml.jackson.annotation.JsonProperty("traceAvailable") boolean traceAvailable, @com.fasterxml.jackson.annotation.JsonProperty("latencyMs") int latencyMs) {
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
     * 返回请求幂等标识。
     *
     * @return 请求幂等标识
     */
    public String requestId() {
        return requestId;
    }

    /**
     * 返回decisionRunId 字段值。
     *
     * @return decisionRunId 字段值
     */
    public String decisionRunId() {
        return decisionRunId;
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
     * 返回strategyVersion 字段值。
     *
     * @return strategyVersion 字段值
     */
    public int strategyVersion() {
        return strategyVersion;
    }

    /**
     * 返回mode 字段值。
     *
     * @return mode 字段值
     */
    public String mode() {
        return mode;
    }

    /**
     * 返回decision 字段值。
     *
     * @return decision 字段值
     */
    public String decision() {
        return decision;
    }

    /**
     * 返回score 字段值。
     *
     * @return score 字段值
     */
    public int score() {
        return score;
    }

    /**
     * 返回riskBand 字段值。
     *
     * @return riskBand 字段值
     */
    public String riskBand() {
        return riskBand;
    }

    /**
     * 返回reasons 字段值。
     *
     * @return reasons 字段值
     */
    public List<String> reasons() {
        return reasons;
    }

    /**
     * 返回matchedRules 字段值。
     *
     * @return matchedRules 字段值
     */
    public List<String> matchedRules() {
        return matchedRules;
    }

    /**
     * 返回labels 字段值。
     *
     * @return labels 字段值
     */
    public List<String> labels() {
        return labels;
    }

    /**
     * 返回missingFeatures 字段值。
     *
     * @return missingFeatures 字段值
     */
    public List<String> missingFeatures() {
        return missingFeatures;
    }

    /**
     * 返回traceAvailable 字段值。
     *
     * @return traceAvailable 字段值
     */
    public boolean traceAvailable() {
        return traceAvailable;
    }

    /**
     * 返回latencyMs 字段值。
     *
     * @return latencyMs 字段值
     */
    public int latencyMs() {
        return latencyMs;
    }

    /**
     * 判断两个 RiskDecisionEvaluateResponse 实例是否包含相同字段值。
     *
     * @param o 待比较对象
     * @return 字段值全部一致时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskDecisionEvaluateResponse that)) {
            return false;
        }
        return java.util.Objects.equals(requestId, that.requestId) && java.util.Objects.equals(decisionRunId, that.decisionRunId) && java.util.Objects.equals(sceneKey, that.sceneKey) && java.util.Objects.equals(strategyKey, that.strategyKey) && java.util.Objects.equals(strategyVersion, that.strategyVersion) && java.util.Objects.equals(mode, that.mode) && java.util.Objects.equals(decision, that.decision) && java.util.Objects.equals(score, that.score) && java.util.Objects.equals(riskBand, that.riskBand) && java.util.Objects.equals(reasons, that.reasons) && java.util.Objects.equals(matchedRules, that.matchedRules) && java.util.Objects.equals(labels, that.labels) && java.util.Objects.equals(missingFeatures, that.missingFeatures) && java.util.Objects.equals(traceAvailable, that.traceAvailable) && java.util.Objects.equals(latencyMs, that.latencyMs);
    }

    /**
     * 根据全部字段生成哈希值。
     *
     * @return 字段哈希值
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(requestId, decisionRunId, sceneKey, strategyKey, strategyVersion, mode, decision, score, riskBand, reasons, matchedRules, labels, missingFeatures, traceAvailable, latencyMs);
    }

    /**
     * 返回与原记录形态一致的调试字符串。
     *
     * @return 字段调试字符串
     */
    @Override
    public String toString() {
        return "RiskDecisionEvaluateResponse[" + "requestId=" + requestId + ", " + "decisionRunId=" + decisionRunId + ", " + "sceneKey=" + sceneKey + ", " + "strategyKey=" + strategyKey + ", " + "strategyVersion=" + strategyVersion + ", " + "mode=" + mode + ", " + "decision=" + decision + ", " + "score=" + score + ", " + "riskBand=" + riskBand + ", " + "reasons=" + reasons + ", " + "matchedRules=" + matchedRules + ", " + "labels=" + labels + ", " + "missingFeatures=" + missingFeatures + ", " + "traceAvailable=" + traceAvailable + ", " + "latencyMs=" + latencyMs + "]";
    }
}
