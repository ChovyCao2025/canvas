package org.chovy.canvas.architecture;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.web.TechnicalMigrationCandidateController;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TechnicalMigrationCandidateTest {

    @Test
    void migrationCreatesEvidenceTableWithTenantAuditAndRollbackFields() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V267__technical_migration_candidate_metrics.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS technical_migration_candidate_evidence")
                .contains("tenant_id BIGINT NOT NULL")
                .contains("candidate_key VARCHAR(128) NOT NULL")
                .contains("proof_command VARCHAR(1000) NOT NULL")
                .contains("baseline_result_json JSON NOT NULL")
                .contains("rollback_command VARCHAR(1000) NOT NULL")
                .contains("decision_status VARCHAR(32) NOT NULL")
                .contains("submitted_by VARCHAR(128) NOT NULL")
                .contains("reviewed_by VARCHAR(128) NULL")
                .contains("reviewed_at DATETIME NULL")
                .contains("idx_migration_candidate_latest");
    }

    @Test
    void registerEvidenceRejectsMissingCandidateProofBaselineOrRollback() {
        TechnicalMigrationCandidateService.EvidenceRepository repository =
                mock(TechnicalMigrationCandidateService.EvidenceRepository.class);
        TechnicalMigrationCandidateService service = new TechnicalMigrationCandidateService(repository);
        TenantContext tenant = new TenantContext(8L, RoleNames.OPERATOR, "operator-1");

        assertThatThrownBy(() -> service.register(tenant, new TechnicalMigrationCandidateService.EvidenceRequest(
                "", "mvn test", "{\"p95\":120}", "git revert abc123", "frontend")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("candidate key is required");

        assertThatThrownBy(() -> service.register(tenant, new TechnicalMigrationCandidateService.EvidenceRequest(
                "virtual-thread-executor", "", "{\"p95\":120}", "git revert abc123", "frontend")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("proof command is required");

        assertThatThrownBy(() -> service.register(tenant, new TechnicalMigrationCandidateService.EvidenceRequest(
                "virtual-thread-executor", "mvn test", "", "git revert abc123", "frontend")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baseline result is required");

        assertThatThrownBy(() -> service.register(tenant, new TechnicalMigrationCandidateService.EvidenceRequest(
                "virtual-thread-executor", "mvn test", "{\"p95\":120}", "", "frontend")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rollback command is required");
    }

    @Test
    void registerEvidenceStoresBlockedDecisionWithTenantAndAuthenticatedOperator() {
        TechnicalMigrationCandidateService.EvidenceRepository repository =
                mock(TechnicalMigrationCandidateService.EvidenceRepository.class);
        TechnicalMigrationCandidateService service = new TechnicalMigrationCandidateService(repository);

        service.register(new TenantContext(8L, RoleNames.OPERATOR, "operator-1"),
                new TechnicalMigrationCandidateService.EvidenceRequest(
                        "rocketmq-topic-split",
                        "cd backend && mvn -pl canvas-engine test -Dtest=MqTriggerConsumerTest",
                        "{\"baseline\":\"BUILD SUCCESS\"}",
                        "restore previous RocketMQ topic config",
                        "spoofed-user"));

        verify(repository).insert(argThat(record ->
                record.tenantId().equals(8L)
                        && record.candidateKey().equals("rocketmq-topic-split")
                        && record.decisionStatus().equals("BLOCKED_PENDING_REVIEW")
                        && record.rollbackCommand().contains("restore previous")
                        && record.submittedBy().equals("operator-1")));
    }

    @Test
    void releaseGateOnlyAllowsReviewedEvidenceWithinTheSameTenant() {
        TechnicalMigrationCandidateService.EvidenceRepository repository =
                mock(TechnicalMigrationCandidateService.EvidenceRepository.class);
        TechnicalMigrationCandidateService service = new TechnicalMigrationCandidateService(repository);
        TenantContext tenant = new TenantContext(8L, RoleNames.OPERATOR, "operator-1");
        when(repository.latest(8L, "spring-mvc-command-dag")).thenReturn(
                new TechnicalMigrationCandidateEvidenceRecord(
                        8L,
                        "spring-mvc-command-dag",
                        "mvn test",
                        "{\"baseline\":\"BUILD SUCCESS\"}",
                        "git revert mvc",
                        "BLOCKED_PENDING_REVIEW",
                        "operator-1"));

        assertThat(service.canStartMigration(tenant, "spring-mvc-command-dag")).isFalse();
    }

    @Test
    void controllerRegistersEvidenceForAuthenticatedTenant() {
        TechnicalMigrationCandidateService service = mock(TechnicalMigrationCandidateService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        TenantContext tenant = new TenantContext(8L, RoleNames.OPERATOR, "operator-1");
        TechnicalMigrationCandidateService.EvidenceRequest request =
                new TechnicalMigrationCandidateService.EvidenceRequest(
                        "powerjob-dynamic-scheduling",
                        "mvn test",
                        "{\"baseline\":\"PASS\"}",
                        "disable PowerJob adapter",
                        "frontend");
        TechnicalMigrationCandidateEvidenceRecord record =
                new TechnicalMigrationCandidateEvidenceRecord(
                        8L,
                        request.candidateKey(),
                        request.proofCommand(),
                        request.baselineResultJson(),
                        request.rollbackCommand(),
                        "BLOCKED_PENDING_REVIEW",
                        "operator-1");
        when(resolver.currentOrError()).thenReturn(Mono.just(tenant));
        when(service.register(tenant, request)).thenReturn(record);
        TechnicalMigrationCandidateController controller = new TechnicalMigrationCandidateController(service, resolver);

        StepVerifier.create(controller.register(request))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData().decisionStatus()).isEqualTo("BLOCKED_PENDING_REVIEW");
                    assertThat(response.getData().submittedBy()).isEqualTo("operator-1");
                })
                .verifyComplete();
    }

    @Test
    void controllerRejectsMissingTenantContext() {
        TechnicalMigrationCandidateService service = mock(TechnicalMigrationCandidateService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.error(new SecurityException("AUTH_003: missing tenant context")));
        TechnicalMigrationCandidateController controller = new TechnicalMigrationCandidateController(service, resolver);

        StepVerifier.create(controller.register(new TechnicalMigrationCandidateService.EvidenceRequest(
                        "powerjob-dynamic-scheduling",
                        "mvn test",
                        "{\"baseline\":\"PASS\"}",
                        "disable PowerJob adapter",
                        "frontend")))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(SecurityException.class)
                        .hasMessageContaining("AUTH_003"))
                .verify();
    }
}
