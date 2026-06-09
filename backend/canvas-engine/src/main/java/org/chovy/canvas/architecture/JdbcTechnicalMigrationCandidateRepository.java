package org.chovy.canvas.architecture;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JdbcTechnicalMigrationCandidateRepository 支撑 architecture 场景的后端处理。
 */
@Repository
public class JdbcTechnicalMigrationCandidateRepository implements TechnicalMigrationCandidateService.EvidenceRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建 JdbcTechnicalMigrationCandidateRepository 实例并注入 architecture 场景依赖。
     * @param jdbcTemplate jdbc template 参数，用于 JdbcTechnicalMigrationCandidateRepository 流程中的校验、计算或对象转换。
     */
    public JdbcTechnicalMigrationCandidateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * insert 处理 architecture 场景的业务逻辑。
     * @param record record 参数，用于 insert 流程中的校验、计算或对象转换。
     */
    @Override
    public void insert(TechnicalMigrationCandidateEvidenceRecord record) {
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
                record.decisionStatus(),
                record.submittedBy());
    }

    /**
     * latest 处理 architecture 场景的业务逻辑。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param candidateKey 业务键，用于在同一租户下定位资源。
     * @return 返回 latest 流程生成的业务结果。
     */
    @Override
    public TechnicalMigrationCandidateEvidenceRecord latest(Long tenantId, String candidateKey) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT tenant_id, candidate_key, proof_command, baseline_result_json,
                           rollback_command, decision_status, submitted_by
                    FROM technical_migration_candidate_evidence
                    WHERE tenant_id = ? AND candidate_key = ?
                    ORDER BY created_at DESC, id DESC
                    LIMIT 1
                    """, (rs, rowNum) -> new TechnicalMigrationCandidateEvidenceRecord(
                    rs.getLong("tenant_id"),
                    rs.getString("candidate_key"),
                    rs.getString("proof_command"),
                    rs.getString("baseline_result_json"),
                    rs.getString("rollback_command"),
                    rs.getString("decision_status"),
                    rs.getString("submitted_by")), tenantId, candidateKey);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }
}
