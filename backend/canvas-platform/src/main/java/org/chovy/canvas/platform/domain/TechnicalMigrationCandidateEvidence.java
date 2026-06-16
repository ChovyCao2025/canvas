package org.chovy.canvas.platform.domain;

import java.util.Objects;

/**
 * 保存技术迁移候选项的评审证据。
 */
public final class TechnicalMigrationCandidateEvidence {

    /**
     * 提交证据所属租户标识。
     */
    private final Long tenantId;

    /**
     * 迁移候选项的稳定键。
     */
    private final String candidateKey;

    /**
     * 用于复现或验证迁移证据的命令。
     */
    private final String proofCommand;

    /**
     * 基线验证结果的 JSON 文本。
     */
    private final String baselineResultJson;

    /**
     * 迁移失败时的回滚命令。
     */
    private final String rollbackCommand;

    /**
     * 当前迁移决策状态。
     */
    private final TechnicalMigrationDecisionStatus decisionStatus;

    /**
     * 提交证据的操作者。
     */
    private final String submittedBy;

    /**
     * 创建技术迁移候选项证据。
     *
     * @param tenantId 提交证据所属租户标识
     * @param candidateKey 迁移候选项的稳定键
     * @param proofCommand 用于复现或验证迁移证据的命令
     * @param baselineResultJson 基线验证结果的 JSON 文本
     * @param rollbackCommand 迁移失败时的回滚命令
     * @param decisionStatus 当前迁移决策状态
     * @param submittedBy 提交证据的操作者
     */
    public TechnicalMigrationCandidateEvidence(
            Long tenantId,
            String candidateKey,
            String proofCommand,
            String baselineResultJson,
            String rollbackCommand,
            TechnicalMigrationDecisionStatus decisionStatus,
            String submittedBy) {
        this.tenantId = tenantId;
        this.candidateKey = candidateKey;
        this.proofCommand = proofCommand;
        this.baselineResultJson = baselineResultJson;
        this.rollbackCommand = rollbackCommand;
        this.decisionStatus = decisionStatus;
        this.submittedBy = submittedBy;
    }

    /**
     * 返回提交证据所属租户标识。
     *
     * @return 提交证据所属租户标识
     */
    public Long tenantId() {
        return tenantId;
    }

    /**
     * 返回迁移候选项的稳定键。
     *
     * @return 迁移候选项的稳定键
     */
    public String candidateKey() {
        return candidateKey;
    }

    /**
     * 返回用于复现或验证迁移证据的命令。
     *
     * @return 用于复现或验证迁移证据的命令
     */
    public String proofCommand() {
        return proofCommand;
    }

    /**
     * 返回基线验证结果的 JSON 文本。
     *
     * @return 基线验证结果的 JSON 文本
     */
    public String baselineResultJson() {
        return baselineResultJson;
    }

    /**
     * 返回迁移失败时的回滚命令。
     *
     * @return 迁移失败时的回滚命令
     */
    public String rollbackCommand() {
        return rollbackCommand;
    }

    /**
     * 返回当前迁移决策状态。
     *
     * @return 当前迁移决策状态
     */
    public TechnicalMigrationDecisionStatus decisionStatus() {
        return decisionStatus;
    }

    /**
     * 返回提交证据的操作者。
     *
     * @return 提交证据的操作者
     */
    public String submittedBy() {
        return submittedBy;
    }

    /**
     * 判断两个迁移证据是否相同。
     *
     * @param object 待比较对象
     * @return 所有字段相同时返回 true
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof TechnicalMigrationCandidateEvidence that)) {
            return false;
        }
        return Objects.equals(tenantId, that.tenantId)
                && Objects.equals(candidateKey, that.candidateKey)
                && Objects.equals(proofCommand, that.proofCommand)
                && Objects.equals(baselineResultJson, that.baselineResultJson)
                && Objects.equals(rollbackCommand, that.rollbackCommand)
                && decisionStatus == that.decisionStatus
                && Objects.equals(submittedBy, that.submittedBy);
    }

    /**
     * 计算迁移证据哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(tenantId, candidateKey, proofCommand, baselineResultJson, rollbackCommand,
                decisionStatus, submittedBy);
    }

    /**
     * 返回与原 record 形态一致的字符串。
     *
     * @return 字符串表示
     */
    @Override
    public String toString() {
        return "TechnicalMigrationCandidateEvidence[tenantId=" + tenantId
                + ", candidateKey=" + candidateKey
                + ", proofCommand=" + proofCommand
                + ", baselineResultJson=" + baselineResultJson
                + ", rollbackCommand=" + rollbackCommand
                + ", decisionStatus=" + decisionStatus
                + ", submittedBy=" + submittedBy + "]";
    }
}
