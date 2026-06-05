package org.chovy.canvas.strategy.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ArchitectureDeploymentEvidenceServiceTest {

    @Test
    void migrationCreatesArchitectureDeploymentEvidenceGate() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V183__architecture_deployment_evidence.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS architecture_deployment_evidence")
                .contains("candidate_key VARCHAR(128) NOT NULL")
                .contains("current_state_evidence TEXT NOT NULL")
                .contains("target_architecture TEXT NOT NULL")
                .contains("scaling_trigger TEXT NOT NULL")
                .contains("dependency_notes TEXT NOT NULL")
                .contains("proof_command VARCHAR(1000) NOT NULL")
                .contains("rollback_plan VARCHAR(1000) NOT NULL")
                .contains("decision_status VARCHAR(32) NOT NULL");
    }

    @Test
    void registerRejectsMissingCurrentStateDependenciesProofOrRollback() {
        ArchitectureDeploymentEvidenceService.EvidenceRepository repository =
                mock(ArchitectureDeploymentEvidenceService.EvidenceRepository.class);
        ArchitectureDeploymentEvidenceService service = new ArchitectureDeploymentEvidenceService(repository);

        assertThatThrownBy(() -> service.register(new ArchitectureDeploymentEvidenceService.EvidenceRequest(
                "service-split-canvas-engine", "architect-1", "", "split execution service",
                "4000 concurrent execution baseline", "ops cost reviewed", "depends on P2-018",
                "mvn test", "deploy monolith artifact")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("current-state evidence is required");

        assertThatThrownBy(() -> service.register(new ArchitectureDeploymentEvidenceService.EvidenceRequest(
                "edge-decisioning", "architect-1", "current API latency captured", "edge worker pilot",
                "regional p95 latency threshold", "ops cost reviewed", "", "mvn test", "disable edge route")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dependency notes are required");
    }

    @Test
    void registerStoresBlockedDecisionUntilArchitectureReview() {
        ArchitectureDeploymentEvidenceService.EvidenceRepository repository =
                mock(ArchitectureDeploymentEvidenceService.EvidenceRepository.class);
        ArchitectureDeploymentEvidenceService service = new ArchitectureDeploymentEvidenceService(repository);

        service.register(new ArchitectureDeploymentEvidenceService.EvidenceRequest(
                "event-driven-delivery", "architect-1", "current delivery outbox metrics captured",
                "separate delivery event topic", "queue lag exceeds reviewed threshold",
                "extra broker operations reviewed", "blocked by P0-003 rollout evidence",
                "cd backend && mvn -pl canvas-engine test -Dtest=MqTriggerConsumerTest",
                "restore previous RocketMQ topic config"));

        verify(repository).insert(argThat(record ->
                record.candidateKey().equals("event-driven-delivery")
                        && record.decisionStatus().equals("BLOCKED_PENDING_REVIEW")
                        && record.dependencyNotes().contains("P0-003")));
    }

    @Test
    void approvalRequiresReviewerAndChildSpec() {
        ArchitectureDeploymentEvidenceService.EvidenceRepository repository =
                mock(ArchitectureDeploymentEvidenceService.EvidenceRepository.class);
        ArchitectureDeploymentEvidenceService service = new ArchitectureDeploymentEvidenceService(repository);

        assertThatThrownBy(() -> service.approve(
                "service-split-canvas-engine", "", "docs/product-evolution/specs/p3-011a-service-boundary-proof.md"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reviewer is required");

        assertThatThrownBy(() -> service.approve("service-split-canvas-engine", "architect-1", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("child spec is required");

        service.approve("service-split-canvas-engine", "architect-1",
                "docs/product-evolution/specs/p3-011a-service-boundary-proof.md");

        verify(repository).approve("service-split-canvas-engine", "architect-1",
                "docs/product-evolution/specs/p3-011a-service-boundary-proof.md");
    }
}
