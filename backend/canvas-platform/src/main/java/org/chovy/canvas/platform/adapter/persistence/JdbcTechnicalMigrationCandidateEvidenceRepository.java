package org.chovy.canvas.platform.adapter.persistence;

import org.chovy.canvas.platform.domain.TechnicalMigrationCandidateEvidence;
import org.chovy.canvas.platform.domain.TechnicalMigrationCandidateEvidenceRepository;
import org.chovy.canvas.platform.domain.TechnicalMigrationDecisionStatus;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 使用 JDBC 持久化技术迁移候选项证据。
 */
@Repository
public class JdbcTechnicalMigrationCandidateEvidenceRepository
        implements TechnicalMigrationCandidateEvidenceRepository {

    /**
     * 执行技术迁移证据 SQL 的 JDBC 模板。
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 使用 JDBC 模板创建仓储。
     *
     * @param jdbcTemplate 执行 SQL 的 JDBC 模板
     */
    public JdbcTechnicalMigrationCandidateEvidenceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 新增技术迁移候选项证据。
     *
     * @param record 待持久化的迁移证据
     */
    @Override
    public void insert(TechnicalMigrationCandidateEvidence record) {
        jdbcTemplate.update("""
                INSERT INTO technical_migration_candidate_evidence (
                    tenant_id,
                    candidate_key,
                    proof_command,
                    baseline_result_json,
                    rollback_command,
                    decision_status,
                    submitted_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                record.tenantId(),
                record.candidateKey(),
                record.proofCommand(),
                record.baselineResultJson(),
                record.rollbackCommand(),
                record.decisionStatus().name(),
                record.submittedBy());
    }

    /**
     * 查询指定候选项的最新证据。
     *
     * @param tenantId 租户标识
     * @param candidateKey 迁移候选项稳定键
     * @return 最新迁移证据；没有记录时返回 null
     */
    @Override
    public TechnicalMigrationCandidateEvidence latest(Long tenantId, String candidateKey) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT tenant_id, candidate_key, proof_command, baseline_result_json,
                           rollback_command, decision_status, submitted_by
                    FROM technical_migration_candidate_evidence
                    WHERE tenant_id = ? AND candidate_key = ?
                    ORDER BY created_at DESC, id DESC
                    LIMIT 1
                    """, (rs, rowNum) -> new TechnicalMigrationCandidateEvidence(
                    rs.getLong("tenant_id"),
                    rs.getString("candidate_key"),
                    rs.getString("proof_command"),
                    rs.getString("baseline_result_json"),
                    rs.getString("rollback_command"),
                    TechnicalMigrationDecisionStatus.valueOf(rs.getString("decision_status")),
                    rs.getString("submitted_by")), tenantId, candidateKey);
        } catch (EmptyResultDataAccessException ignored) {
            // 未提交证据时返回 null，调用方据此判断迁移尚未获准。
            return null;
        }
    }
}
