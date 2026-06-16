package org.chovy.canvas.risk.domain.runtime;

import java.util.Objects;

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
public final class RiskDecisionRunRecord {

    /**
     * RiskDecisionRunRecord 的 decisionRunId 字段。
     */
    private final String decisionRunId;


    /**
     * RiskDecisionRunRecord 的 tenantId 字段。
     */
    private final Long tenantId;


    /**
     * RiskDecisionRunRecord 的 requestId 字段。
     */
    private final String requestId;


    /**
     * RiskDecisionRunRecord 的 requestHash 字段。
     */
    private final String requestHash;


    /**
     * RiskDecisionRunRecord 的 subjectHash 字段。
     */
    private final String subjectHash;


    /**
     * RiskDecisionRunRecord 的 inputSnapshotJson 字段。
     */
    private final String inputSnapshotJson;


    /**
     * RiskDecisionRunRecord 的 response 字段。
     */
    private final RiskDecisionResponse response;


    /**
     * 创建 RiskDecisionRunRecord。
     *
     * @param decisionRunId RiskDecisionRunRecord 的 decisionRunId 字段
     * @param tenantId RiskDecisionRunRecord 的 tenantId 字段
     * @param requestId RiskDecisionRunRecord 的 requestId 字段
     * @param requestHash RiskDecisionRunRecord 的 requestHash 字段
     * @param subjectHash RiskDecisionRunRecord 的 subjectHash 字段
     * @param inputSnapshotJson RiskDecisionRunRecord 的 inputSnapshotJson 字段
     * @param response RiskDecisionRunRecord 的 response 字段
     */
    public RiskDecisionRunRecord(String decisionRunId, Long tenantId, String requestId, String requestHash, String subjectHash, String inputSnapshotJson, RiskDecisionResponse response) {
        this.decisionRunId = decisionRunId;
        this.tenantId = tenantId;
        this.requestId = requestId;
        this.requestHash = requestHash;
        this.subjectHash = subjectHash;
        this.inputSnapshotJson = inputSnapshotJson;
        this.response = response;
    }

    /**
     * 返回 RiskDecisionRunRecord 的 decisionRunId 字段。
     *
     * @return decisionRunId 字段值
     */
    public String decisionRunId() {
        return decisionRunId;
    }

    /**
     * 返回 RiskDecisionRunRecord 的 tenantId 字段。
     *
     * @return tenantId 字段值
     */
    public Long tenantId() {
        return tenantId;
    }

    /**
     * 返回 RiskDecisionRunRecord 的 requestId 字段。
     *
     * @return requestId 字段值
     */
    public String requestId() {
        return requestId;
    }

    /**
     * 返回 RiskDecisionRunRecord 的 requestHash 字段。
     *
     * @return requestHash 字段值
     */
    public String requestHash() {
        return requestHash;
    }

    /**
     * 返回 RiskDecisionRunRecord 的 subjectHash 字段。
     *
     * @return subjectHash 字段值
     */
    public String subjectHash() {
        return subjectHash;
    }

    /**
     * 返回 RiskDecisionRunRecord 的 inputSnapshotJson 字段。
     *
     * @return inputSnapshotJson 字段值
     */
    public String inputSnapshotJson() {
        return inputSnapshotJson;
    }

    /**
     * 返回 RiskDecisionRunRecord 的 response 字段。
     *
     * @return response 字段值
     */
    public RiskDecisionResponse response() {
        return response;
    }

    /**
     * 比较当前 RiskDecisionRunRecord 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskDecisionRunRecord other)) {
            return false;
        }
        return Objects.equals(decisionRunId, other.decisionRunId)
                && Objects.equals(tenantId, other.tenantId)
                && Objects.equals(requestId, other.requestId)
                && Objects.equals(requestHash, other.requestHash)
                && Objects.equals(subjectHash, other.subjectHash)
                && Objects.equals(inputSnapshotJson, other.inputSnapshotJson)
                && Objects.equals(response, other.response);
    }

    /**
     * 计算 RiskDecisionRunRecord 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(decisionRunId, tenantId, requestId, requestHash, subjectHash, inputSnapshotJson, response);
    }

    /**
     * 返回 RiskDecisionRunRecord 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskDecisionRunRecord[decisionRunId=" + decisionRunId + ", tenantId=" + tenantId + ", requestId=" + requestId + ", requestHash=" + requestHash + ", subjectHash=" + subjectHash + ", inputSnapshotJson=" + inputSnapshotJson + ", response=" + response + "]";
    }

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
