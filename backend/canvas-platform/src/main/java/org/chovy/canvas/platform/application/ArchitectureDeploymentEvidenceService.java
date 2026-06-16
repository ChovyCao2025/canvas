package org.chovy.canvas.platform.application;

import org.chovy.canvas.platform.api.ArchitectureDeploymentEvidenceFacade;
import org.chovy.canvas.platform.api.ArchitectureDeploymentEvidenceRequest;
import org.chovy.canvas.platform.api.ArchitectureDeploymentEvidenceView;
import org.chovy.canvas.platform.domain.ArchitectureDeploymentEvidence;
import org.chovy.canvas.platform.domain.ArchitectureDeploymentEvidenceRepository;
import org.springframework.stereotype.Service;

/**
 * 处理架构部署证据登记和审批的应用服务。
 */
@Service
public class ArchitectureDeploymentEvidenceService implements ArchitectureDeploymentEvidenceFacade {

    /**
     * 新证据登记后的默认决策状态。
     */
    private static final String BLOCKED_PENDING_REVIEW = "BLOCKED_PENDING_REVIEW";

    /**
     * 持久化架构部署证据的仓储。
     */
    private final ArchitectureDeploymentEvidenceRepository repository;

    /**
     * 使用证据仓储创建应用服务。
     *
     * @param repository 持久化架构部署证据的仓储
     */
    public ArchitectureDeploymentEvidenceService(ArchitectureDeploymentEvidenceRepository repository) {
        this.repository = repository;
    }

    /**
     * 登记架构部署证据。
     *
     * {@inheritDoc}
     */
    @Override
    public ArchitectureDeploymentEvidenceView register(ArchitectureDeploymentEvidenceRequest request) {
        requireText(request.candidateKey(), "candidate key is required");
        requireText(request.ownerId(), "owner is required");
        requireText(request.currentStateEvidence(), "current-state evidence is required");
        requireText(request.targetArchitecture(), "target architecture is required");
        requireText(request.scalingTrigger(), "scaling trigger is required");
        requireText(request.operationalCostNotes(), "operational cost notes are required");
        requireText(request.dependencyNotes(), "dependency notes are required");
        requireText(request.proofCommand(), "proof command is required");
        requireText(request.rollbackPlan(), "rollback plan is required");

        // 入库前统一 trim，避免评审键和证据文本因边界空白产生重复记录。
        ArchitectureDeploymentEvidence record = new ArchitectureDeploymentEvidence(
                request.candidateKey().trim(),
                request.ownerId().trim(),
                request.currentStateEvidence().trim(),
                request.targetArchitecture().trim(),
                request.scalingTrigger().trim(),
                request.operationalCostNotes().trim(),
                request.dependencyNotes().trim(),
                request.proofCommand().trim(),
                request.rollbackPlan().trim(),
                BLOCKED_PENDING_REVIEW);
        repository.insert(record);
        return toView(record);
    }

    /**
     * 审批候选架构。
     *
     * {@inheritDoc}
     */
    @Override
    public void approve(String candidateKey, String reviewerId, String childSpec) {
        requireText(candidateKey, "candidate key is required");
        requireText(reviewerId, "reviewer is required");
        requireText(childSpec, "child spec is required");
        repository.approve(candidateKey.trim(), reviewerId.trim(), childSpec.trim());
    }

    /**
     * 将领域证据转换为公开 API 视图。
     *
     * @param record 领域证据记录
     * @return 公开 API 视图
     */
    private static ArchitectureDeploymentEvidenceView toView(ArchitectureDeploymentEvidence record) {
        return new ArchitectureDeploymentEvidenceView(
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
     * 校验文本字段必须存在且不能只包含空白字符。
     *
     * @param value 待校验文本
     * @param message 校验失败时使用的异常消息
     * @throws IllegalArgumentException 当文本为空或空白时抛出
     */
    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
