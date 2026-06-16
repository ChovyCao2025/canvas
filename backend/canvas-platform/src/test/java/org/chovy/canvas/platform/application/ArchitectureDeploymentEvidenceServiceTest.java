package org.chovy.canvas.platform.application;

import org.chovy.canvas.platform.api.ArchitectureDeploymentEvidenceRequest;
import org.chovy.canvas.platform.domain.ArchitectureDeploymentEvidence;
import org.chovy.canvas.platform.domain.ArchitectureDeploymentEvidenceRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 覆盖架构部署证据服务的登记校验和审批校验。
 */
class ArchitectureDeploymentEvidenceServiceTest {

    /**
     * 验证登记时必须提供证据和回滚信息，并写入待评审状态。
     */
    @Test
    void registerRequiresProofAndRollbackEvidenceThenStoresPendingReview() {
        ArchitectureDeploymentEvidenceRepository repository = mock(ArchitectureDeploymentEvidenceRepository.class);
        ArchitectureDeploymentEvidenceService service = new ArchitectureDeploymentEvidenceService(repository);

        var view = service.register(new ArchitectureDeploymentEvidenceRequest(
                "virtual-thread-executor",
                "platform-worker",
                "{\"p95\":120}",
                "bounded platform module",
                "queue depth over 1000",
                "lower thread scheduling cost",
                "depends on Java 21",
                "cd backend && mvn test -pl canvas-platform",
                "disable virtual thread executor"));

        assertThat(view.decisionStatus()).isEqualTo("BLOCKED_PENDING_REVIEW");
        verify(repository).insert(argThat(record ->
                record.candidateKey().equals("virtual-thread-executor")
                        && record.decisionStatus().equals("BLOCKED_PENDING_REVIEW")));
    }

    /**
     * 验证登记缺失必填字段时快速失败。
     */
    @Test
    void registerRejectsMissingRequiredFields() {
        ArchitectureDeploymentEvidenceService service =
                new ArchitectureDeploymentEvidenceService(mock(ArchitectureDeploymentEvidenceRepository.class));

        assertThatThrownBy(() -> service.register(new ArchitectureDeploymentEvidenceRequest(
                "", "owner", "state", "target", "trigger", "cost", "deps", "proof", "rollback")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("candidate key is required");
        assertThatThrownBy(() -> service.register(new ArchitectureDeploymentEvidenceRequest(
                "candidate", "owner", "state", "target", "trigger", "cost", "deps", "", "rollback")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("proof command is required");
        assertThatThrownBy(() -> service.register(new ArchitectureDeploymentEvidenceRequest(
                "candidate", "owner", "state", "target", "trigger", "cost", "deps", "proof", "")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rollback plan is required");
    }

    /**
     * 验证审批必须提供评审人和子规格。
     */
    @Test
    void approveRequiresReviewerAndChildSpec() {
        ArchitectureDeploymentEvidenceRepository repository = mock(ArchitectureDeploymentEvidenceRepository.class);
        ArchitectureDeploymentEvidenceService service = new ArchitectureDeploymentEvidenceService(repository);

        service.approve("virtual-thread-executor", "reviewer-1", "docs/ddd-rewrite/child-specs/platform.md");

        verify(repository).approve("virtual-thread-executor", "reviewer-1", "docs/ddd-rewrite/child-specs/platform.md");
        assertThatThrownBy(() -> service.approve("virtual-thread-executor", "", "docs/spec.md"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reviewer is required");
        assertThatThrownBy(() -> service.approve("virtual-thread-executor", "reviewer-1", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("child spec is required");
    }
}
