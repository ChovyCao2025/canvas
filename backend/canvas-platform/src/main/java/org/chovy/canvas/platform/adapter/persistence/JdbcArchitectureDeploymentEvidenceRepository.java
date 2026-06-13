package org.chovy.canvas.platform.adapter.persistence;

import org.chovy.canvas.platform.domain.ArchitectureDeploymentEvidence;
import org.chovy.canvas.platform.domain.ArchitectureDeploymentEvidenceRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcArchitectureDeploymentEvidenceRepository implements ArchitectureDeploymentEvidenceRepository {

    private static final String APPROVED_FOR_CHILD_SPEC = "APPROVED_FOR_CHILD_SPEC";

    private final JdbcTemplate jdbcTemplate;

    public JdbcArchitectureDeploymentEvidenceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insert(ArchitectureDeploymentEvidence record) {
        jdbcTemplate.update("""
                INSERT INTO architecture_deployment_evidence (
                    candidate_key,
                    owner_id,
                    current_state_evidence,
                    target_architecture,
                    scaling_trigger,
                    operational_cost_notes,
                    dependency_notes,
                    proof_command,
                    rollback_plan,
                    decision_status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                record.candidateKey(),
                record.ownerId(),
                record.currentStateEvidence(),
                record.targetArchitecture(),
                record.scalingTrigger(),
                record.operationalCostNotes(),
                record.dependencyNotes(),
                record.proofCommand(),
                record.rollbackPlan(),
                record.decisionStatus());
    }

    @Override
    public void approve(String candidateKey, String reviewerId, String childSpec) {
        jdbcTemplate.update("""
                UPDATE architecture_deployment_evidence
                SET decision_status = ?,
                    reviewed_by = ?,
                    reviewed_at = CURRENT_TIMESTAMP,
                    child_spec = ?
                WHERE candidate_key = ?
                """, APPROVED_FOR_CHILD_SPEC, reviewerId, childSpec, candidateKey);
    }
}
