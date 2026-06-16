package org.chovy.canvas.platform.adapter.persistence;

import org.chovy.canvas.platform.domain.ArchitectureDeploymentEvidence;
import org.chovy.canvas.platform.domain.ArchitectureDeploymentEvidenceRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 使用 JDBC 持久化架构部署证据。
 */
@Repository
public class JdbcArchitectureDeploymentEvidenceRepository implements ArchitectureDeploymentEvidenceRepository {

    /**
     * 审批通过后写入数据库的决策状态。
     */
    private static final String APPROVED_FOR_CHILD_SPEC = "APPROVED_FOR_CHILD_SPEC";

    /**
     * 执行架构部署证据 SQL 的 JDBC 模板。
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 使用 JDBC 模板创建仓储。
     *
     * @param jdbcTemplate 执行 SQL 的 JDBC 模板
     */
    public JdbcArchitectureDeploymentEvidenceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 新增架构部署证据。
     *
     * @param record 待持久化的架构部署证据
     */
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

    /**
     * 将候选架构标记为已批准并记录评审信息。
     *
     * @param candidateKey 候选架构稳定键
     * @param reviewerId 评审人标识
     * @param childSpec 审批后关联的子规格
     */
    @Override
    public void approve(String candidateKey, String reviewerId, String childSpec) {
        // 使用数据库当前时间记录审批时刻，避免应用节点时钟差异影响审计。
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
