package org.chovy.canvas.web.risk;

import java.util.List;

/**
 * 风控决策追踪视图，供工作台读取最近决策和规则命中证据。
 *
 * @param traceId 决策运行编号
 * @param requestId 调用方幂等请求编号
 * @param sceneKey 场景业务键
 * @param strategyKey 策略业务键
 * @param strategyVersion 策略版本
 * @param mode 运行模式
 * @param decision 最终决策动作
 * @param score 风险分
 * @param riskBand 风险等级
 * @param latencyMs 决策耗时
 * @param createdAt 创建时间
 * @param matchedRules 命中规则列表
 */
public final class RiskDecisionTraceView {

    /**
     * traceId 字段值。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("traceId")
    private final String traceId;

    /**
     * 请求幂等标识。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("requestId")
    private final String requestId;

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
    private final Integer strategyVersion;

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
    private final Integer score;

    /**
     * riskBand 字段值。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("riskBand")
    private final String riskBand;

    /**
     * latencyMs 字段值。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("latencyMs")
    private final Integer latencyMs;

    /**
     * createdAt 字段值。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("createdAt")
    private final String createdAt;

    /**
     * matchedRules 字段值。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("matchedRules")
    private final List<String> matchedRules;

    /**
     * 创建 RiskDecisionTraceView 实例。
     *
     * @param traceId traceId 字段值
     * @param requestId 请求幂等标识
     * @param sceneKey 场景业务键
     * @param strategyKey 策略业务键
     * @param strategyVersion strategyVersion 字段值
     * @param mode mode 字段值
     * @param decision decision 字段值
     * @param score score 字段值
     * @param riskBand riskBand 字段值
     * @param latencyMs latencyMs 字段值
     * @param createdAt createdAt 字段值
     * @param matchedRules matchedRules 字段值
     */
    @com.fasterxml.jackson.annotation.JsonCreator
    public RiskDecisionTraceView(@com.fasterxml.jackson.annotation.JsonProperty("traceId") String traceId, @com.fasterxml.jackson.annotation.JsonProperty("requestId") String requestId, @com.fasterxml.jackson.annotation.JsonProperty("sceneKey") String sceneKey, @com.fasterxml.jackson.annotation.JsonProperty("strategyKey") String strategyKey, @com.fasterxml.jackson.annotation.JsonProperty("strategyVersion") Integer strategyVersion, @com.fasterxml.jackson.annotation.JsonProperty("mode") String mode, @com.fasterxml.jackson.annotation.JsonProperty("decision") String decision, @com.fasterxml.jackson.annotation.JsonProperty("score") Integer score, @com.fasterxml.jackson.annotation.JsonProperty("riskBand") String riskBand, @com.fasterxml.jackson.annotation.JsonProperty("latencyMs") Integer latencyMs, @com.fasterxml.jackson.annotation.JsonProperty("createdAt") String createdAt, @com.fasterxml.jackson.annotation.JsonProperty("matchedRules") List<String> matchedRules) {
        this.traceId = traceId;
        this.requestId = requestId;
        this.sceneKey = sceneKey;
        this.strategyKey = strategyKey;
        this.strategyVersion = strategyVersion;
        this.mode = mode;
        this.decision = decision;
        this.score = score;
        this.riskBand = riskBand;
        this.latencyMs = latencyMs;
        this.createdAt = createdAt;
        this.matchedRules = matchedRules;
    }

    /**
     * 返回traceId 字段值。
     *
     * @return traceId 字段值
     */
    public String traceId() {
        return traceId;
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
    public Integer strategyVersion() {
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
    public Integer score() {
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
     * 返回latencyMs 字段值。
     *
     * @return latencyMs 字段值
     */
    public Integer latencyMs() {
        return latencyMs;
    }

    /**
     * 返回createdAt 字段值。
     *
     * @return createdAt 字段值
     */
    public String createdAt() {
        return createdAt;
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
     * 判断两个 RiskDecisionTraceView 实例是否包含相同字段值。
     *
     * @param o 待比较对象
     * @return 字段值全部一致时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskDecisionTraceView that)) {
            return false;
        }
        return java.util.Objects.equals(traceId, that.traceId) && java.util.Objects.equals(requestId, that.requestId) && java.util.Objects.equals(sceneKey, that.sceneKey) && java.util.Objects.equals(strategyKey, that.strategyKey) && java.util.Objects.equals(strategyVersion, that.strategyVersion) && java.util.Objects.equals(mode, that.mode) && java.util.Objects.equals(decision, that.decision) && java.util.Objects.equals(score, that.score) && java.util.Objects.equals(riskBand, that.riskBand) && java.util.Objects.equals(latencyMs, that.latencyMs) && java.util.Objects.equals(createdAt, that.createdAt) && java.util.Objects.equals(matchedRules, that.matchedRules);
    }

    /**
     * 根据全部字段生成哈希值。
     *
     * @return 字段哈希值
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(traceId, requestId, sceneKey, strategyKey, strategyVersion, mode, decision, score, riskBand, latencyMs, createdAt, matchedRules);
    }

    /**
     * 返回与原记录形态一致的调试字符串。
     *
     * @return 字段调试字符串
     */
    @Override
    public String toString() {
        return "RiskDecisionTraceView[" + "traceId=" + traceId + ", " + "requestId=" + requestId + ", " + "sceneKey=" + sceneKey + ", " + "strategyKey=" + strategyKey + ", " + "strategyVersion=" + strategyVersion + ", " + "mode=" + mode + ", " + "decision=" + decision + ", " + "score=" + score + ", " + "riskBand=" + riskBand + ", " + "latencyMs=" + latencyMs + ", " + "createdAt=" + createdAt + ", " + "matchedRules=" + matchedRules + "]";
    }
}
