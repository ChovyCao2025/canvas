package org.chovy.canvas.domain.risk.runtime;

/**
 * 风控决策运行记录。
 *
 * @param decisionRunId 决策运行编号
 * @param tenantId 租户编号
 * @param requestId 请求幂等编号
 * @param requestHash 请求载荷哈希
 * @param subjectHash 主体哈希
 * @param inputSnapshotJson 输入快照 JSON
 * @param response 决策响应
 */
public record RiskDecisionRunRecord(
        String decisionRunId,
        Long tenantId,
        String requestId,
        String requestHash,
        String subjectHash,
        String inputSnapshotJson,
        RiskDecisionResponse response
) {

    /**
     * 创建兼容旧调用方的运行记录，主体哈希默认使用请求哈希。
     */
    public RiskDecisionRunRecord(String decisionRunId,
                                 Long tenantId,
                                 String requestId,
                                 String requestHash,
                                 String inputSnapshotJson,
                                 RiskDecisionResponse response) {
        this(decisionRunId, tenantId, requestId, requestHash, requestHash, inputSnapshotJson, response);
    }

    /**
     * 返回替换决策运行编号后的记录副本，并同步响应中的运行编号。
     */
    public RiskDecisionRunRecord withDecisionRunId(String newDecisionRunId) {
        return new RiskDecisionRunRecord(newDecisionRunId, tenantId, requestId, requestHash, subjectHash,
                inputSnapshotJson, response == null ? null : new RiskDecisionResponse(
                        response.requestId(),
                        newDecisionRunId,
                        response.sceneKey(),
                        response.strategyKey(),
                        response.strategyVersion(),
                        response.mode(),
                        response.action(),
                        response.score(),
                        response.riskBand(),
                        response.reasons(),
                        response.matchedRules(),
                        response.labels(),
                        response.missingFeatures(),
                        response.latencyMs(),
                        response.traceAvailable()));
    }
}
