package org.chovy.canvas.platform.application;

import org.chovy.canvas.platform.api.PlatformActor;
import org.chovy.canvas.platform.api.TechnicalMigrationEvidenceRequest;
import org.chovy.canvas.platform.domain.TechnicalMigrationCandidateEvidence;
import org.chovy.canvas.platform.domain.TechnicalMigrationCandidateEvidenceRepository;
import org.chovy.canvas.platform.domain.TechnicalMigrationDecisionStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 覆盖技术迁移候选项服务的证据登记和迁移准入。
 */
class TechnicalMigrationCandidateApplicationServiceTest {

    /**
     * 验证登记证据会写入租户、操作者和待复核决策。
     */
    @Test
    void registerEvidenceStoresBlockedDecisionWithTenantAndActor() {
        TechnicalMigrationCandidateEvidenceRepository repository =
                mock(TechnicalMigrationCandidateEvidenceRepository.class);
        TechnicalMigrationCandidateApplicationService service =
                new TechnicalMigrationCandidateApplicationService(repository);

        service.register(new PlatformActor(8L, "operator-1"), new TechnicalMigrationEvidenceRequest(
                "RocketMQ-Topic-Split",
                "cd backend && mvn -pl canvas-engine test -Dtest=MqTriggerConsumerTest",
                "{\"baseline\":\"BUILD SUCCESS\"}",
                "restore previous RocketMQ topic config"));

        verify(repository).insert(argThat(record ->
                record.tenantId().equals(8L)
                        && record.candidateKey().equals("rocketmq-topic-split")
                        && record.decisionStatus() == TechnicalMigrationDecisionStatus.BLOCKED_PENDING_REVIEW
                        && record.rollbackCommand().contains("restore previous")
                        && record.submittedBy().equals("operator-1")));
    }

    /**
     * 验证缺失租户、候选项、证明、基线或回滚命令时拒绝登记。
     */
    @Test
    void registerEvidenceRejectsMissingTenantCandidateProofBaselineOrRollback() {
        TechnicalMigrationCandidateApplicationService service =
                new TechnicalMigrationCandidateApplicationService(mock(TechnicalMigrationCandidateEvidenceRepository.class));
        PlatformActor actor = new PlatformActor(8L, "operator-1");

        assertThatThrownBy(() -> service.register(new PlatformActor(null, "operator-1"),
                request("candidate", "mvn test", "{\"pass\":true}", "git revert abc123")))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("AUTH_003");
        assertThatThrownBy(() -> service.register(actor, request("", "mvn test", "{\"pass\":true}", "git revert abc123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("candidate key is required");
        assertThatThrownBy(() -> service.register(actor, request("candidate", "", "{\"pass\":true}", "git revert abc123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("proof command is required");
        assertThatThrownBy(() -> service.register(actor, request("candidate", "mvn test", "", "git revert abc123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baseline result is required");
        assertThatThrownBy(() -> service.register(actor, request("candidate", "mvn test", "{\"pass\":true}", "")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rollback command is required");
    }

    /**
     * 验证只有同租户已评审通过的证据允许开始迁移。
     */
    @Test
    void releaseGateOnlyAllowsReviewedEvidenceWithinSameTenant() {
        TechnicalMigrationCandidateEvidenceRepository repository =
                mock(TechnicalMigrationCandidateEvidenceRepository.class);
        TechnicalMigrationCandidateApplicationService service =
                new TechnicalMigrationCandidateApplicationService(repository);
        when(repository.latest(8L, "spring-mvc-command-dag")).thenReturn(
                new TechnicalMigrationCandidateEvidence(
                        8L,
                        "spring-mvc-command-dag",
                        "mvn test",
                        "{\"baseline\":\"BUILD SUCCESS\"}",
                        "git revert mvc",
                        TechnicalMigrationDecisionStatus.BLOCKED_PENDING_REVIEW,
                        "operator-1"));

        assertThat(service.canStartMigration(new PlatformActor(8L, "operator-1"), "spring-mvc-command-dag"))
                .isFalse();

        when(repository.latest(8L, "spring-mvc-command-dag")).thenReturn(
                new TechnicalMigrationCandidateEvidence(
                        8L,
                        "spring-mvc-command-dag",
                        "mvn test",
                        "{\"baseline\":\"BUILD SUCCESS\"}",
                        "git revert mvc",
                        TechnicalMigrationDecisionStatus.APPROVED_FOR_CHILD_SPEC,
                        "reviewer-1"));

        assertThat(service.canStartMigration(new PlatformActor(8L, "operator-1"), "spring-mvc-command-dag"))
                .isTrue();
    }

    /**
     * 构造迁移证据请求。
     *
     * @param candidateKey 迁移候选项键
     * @param proofCommand 证明命令
     * @param baselineResultJson 基线结果 JSON
     * @param rollbackCommand 回滚命令
     * @return 迁移证据请求
     */
    private static TechnicalMigrationEvidenceRequest request(
            String candidateKey,
            String proofCommand,
            String baselineResultJson,
            String rollbackCommand) {
        return new TechnicalMigrationEvidenceRequest(candidateKey, proofCommand, baselineResultJson, rollbackCommand);
    }
}
