package org.chovy.canvas.platform.api;

import java.util.Objects;

/**
 * 提交技术迁移候选项证据时使用的请求数据。
 */
public final class TechnicalMigrationEvidenceRequest {

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
     * 创建技术迁移证据请求。
     *
     * @param candidateKey 迁移候选项的稳定键
     * @param proofCommand 用于复现或验证迁移证据的命令
     * @param baselineResultJson 基线验证结果的 JSON 文本
     * @param rollbackCommand 迁移失败时的回滚命令
     */
    public TechnicalMigrationEvidenceRequest(
            String candidateKey,
            String proofCommand,
            String baselineResultJson,
            String rollbackCommand) {
        this.candidateKey = candidateKey;
        this.proofCommand = proofCommand;
        this.baselineResultJson = baselineResultJson;
        this.rollbackCommand = rollbackCommand;
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
        if (!(object instanceof TechnicalMigrationEvidenceRequest that)) {
            return false;
        }
        return Objects.equals(candidateKey, that.candidateKey)
                && Objects.equals(proofCommand, that.proofCommand)
                && Objects.equals(baselineResultJson, that.baselineResultJson)
                && Objects.equals(rollbackCommand, that.rollbackCommand);
    }

    /**
     * 计算请求值对象哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(candidateKey, proofCommand, baselineResultJson, rollbackCommand);
    }

    /**
     * 返回与原 record 形态一致的字符串。
     *
     * @return 字符串表示
     */
    @Override
    public String toString() {
        return "TechnicalMigrationEvidenceRequest[candidateKey=" + candidateKey
                + ", proofCommand=" + proofCommand
                + ", baselineResultJson=" + baselineResultJson
                + ", rollbackCommand=" + rollbackCommand + "]";
    }
}
