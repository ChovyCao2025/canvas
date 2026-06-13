package org.chovy.canvas.platform.adapter.persistence;

import org.chovy.canvas.platform.domain.TechnicalMigrationCandidateEvidence;
import org.chovy.canvas.platform.domain.TechnicalMigrationCandidateEvidenceRepository;
import org.chovy.canvas.platform.domain.TechnicalMigrationDecisionStatus;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTechnicalMigrationCandidateEvidenceRepository
        implements TechnicalMigrationCandidateEvidenceRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcTechnicalMigrationCandidateEvidenceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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
            return null;
        }
    }
}
