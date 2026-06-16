package org.chovy.canvas.platform.api;

import java.util.Objects;

/**
 * 提交架构部署证据时使用的请求数据。
 */
public final class ArchitectureDeploymentEvidenceRequest {

    /**
     * 候选架构方案的稳定键。
     */
    private final String candidateKey;

    /**
     * 负责该部署证据的负责人标识。
     */
    private final String ownerId;

    /**
     * 当前系统状态的证据说明。
     */
    private final String currentStateEvidence;

    /**
     * 目标架构形态说明。
     */
    private final String targetArchitecture;

    /**
     * 触发扩容或架构切换的条件。
     */
    private final String scalingTrigger;

    /**
     * 运维成本和资源影响说明。
     */
    private final String operationalCostNotes;

    /**
     * 外部依赖或内部依赖说明。
     */
    private final String dependencyNotes;

    /**
     * 用于验证证据的命令。
     */
    private final String proofCommand;

    /**
     * 部署失败时的回滚方案。
     */
    private final String rollbackPlan;

    /**
     * 创建架构部署证据请求。
     *
     * @param candidateKey 候选架构方案的稳定键
     * @param ownerId 负责该部署证据的负责人标识
     * @param currentStateEvidence 当前系统状态的证据说明
     * @param targetArchitecture 目标架构形态说明
     * @param scalingTrigger 触发扩容或架构切换的条件
     * @param operationalCostNotes 运维成本和资源影响说明
     * @param dependencyNotes 外部依赖或内部依赖说明
     * @param proofCommand 用于验证证据的命令
     * @param rollbackPlan 部署失败时的回滚方案
     */
    public ArchitectureDeploymentEvidenceRequest(
            String candidateKey,
            String ownerId,
            String currentStateEvidence,
            String targetArchitecture,
            String scalingTrigger,
            String operationalCostNotes,
            String dependencyNotes,
            String proofCommand,
            String rollbackPlan) {
        this.candidateKey = candidateKey;
        this.ownerId = ownerId;
        this.currentStateEvidence = currentStateEvidence;
        this.targetArchitecture = targetArchitecture;
        this.scalingTrigger = scalingTrigger;
        this.operationalCostNotes = operationalCostNotes;
        this.dependencyNotes = dependencyNotes;
        this.proofCommand = proofCommand;
        this.rollbackPlan = rollbackPlan;
    }

    /**
     * 返回候选架构方案的稳定键。
     *
     * @return 候选架构方案的稳定键
     */
    public String candidateKey() {
        return candidateKey;
    }

    /**
     * 返回负责该部署证据的负责人标识。
     *
     * @return 负责该部署证据的负责人标识
     */
    public String ownerId() {
        return ownerId;
    }

    /**
     * 返回当前系统状态的证据说明。
     *
     * @return 当前系统状态的证据说明
     */
    public String currentStateEvidence() {
        return currentStateEvidence;
    }

    /**
     * 返回目标架构形态说明。
     *
     * @return 目标架构形态说明
     */
    public String targetArchitecture() {
        return targetArchitecture;
    }

    /**
     * 返回触发扩容或架构切换的条件。
     *
     * @return 触发扩容或架构切换的条件
     */
    public String scalingTrigger() {
        return scalingTrigger;
    }

    /**
     * 返回运维成本和资源影响说明。
     *
     * @return 运维成本和资源影响说明
     */
    public String operationalCostNotes() {
        return operationalCostNotes;
    }

    /**
     * 返回外部依赖或内部依赖说明。
     *
     * @return 外部依赖或内部依赖说明
     */
    public String dependencyNotes() {
        return dependencyNotes;
    }

    /**
     * 返回用于验证证据的命令。
     *
     * @return 用于验证证据的命令
     */
    public String proofCommand() {
        return proofCommand;
    }

    /**
     * 返回部署失败时的回滚方案。
     *
     * @return 部署失败时的回滚方案
     */
    public String rollbackPlan() {
        return rollbackPlan;
    }

    /**
     * 判断两个请求值对象是否相同。
     *
     * @param object 待比较对象
     * @return 所有字段相同时返回 true
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ArchitectureDeploymentEvidenceRequest that)) {
            return false;
        }
        return Objects.equals(candidateKey, that.candidateKey)
                && Objects.equals(ownerId, that.ownerId)
                && Objects.equals(currentStateEvidence, that.currentStateEvidence)
                && Objects.equals(targetArchitecture, that.targetArchitecture)
                && Objects.equals(scalingTrigger, that.scalingTrigger)
                && Objects.equals(operationalCostNotes, that.operationalCostNotes)
                && Objects.equals(dependencyNotes, that.dependencyNotes)
                && Objects.equals(proofCommand, that.proofCommand)
                && Objects.equals(rollbackPlan, that.rollbackPlan);
    }

    /**
     * 计算请求值对象哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(candidateKey, ownerId, currentStateEvidence, targetArchitecture, scalingTrigger,
                operationalCostNotes, dependencyNotes, proofCommand, rollbackPlan);
    }

    /**
     * 返回与原 record 形态一致的字符串。
     *
     * @return 字符串表示
     */
    @Override
    public String toString() {
        return "ArchitectureDeploymentEvidenceRequest[candidateKey=" + candidateKey
                + ", ownerId=" + ownerId
                + ", currentStateEvidence=" + currentStateEvidence
                + ", targetArchitecture=" + targetArchitecture
                + ", scalingTrigger=" + scalingTrigger
                + ", operationalCostNotes=" + operationalCostNotes
                + ", dependencyNotes=" + dependencyNotes
                + ", proofCommand=" + proofCommand
                + ", rollbackPlan=" + rollbackPlan + "]";
    }
}
