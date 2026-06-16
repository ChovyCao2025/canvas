package org.chovy.canvas.platform.domain;

/**
 * 持久化技术迁移候选项证据的仓储接口。
 */
public interface TechnicalMigrationCandidateEvidenceRepository {

    /**
     * 新增一条技术迁移候选项证据记录。
     *
     * @param record 待持久化的迁移证据
     */
    void insert(TechnicalMigrationCandidateEvidence record);

    /**
     * 查询指定租户和候选项的最新迁移证据。
     *
     * @param tenantId 租户标识
     * @param candidateKey 迁移候选项的稳定键
     * @return 最新迁移证据；没有记录时由实现决定返回值
     */
    TechnicalMigrationCandidateEvidence latest(Long tenantId, String candidateKey);
}
