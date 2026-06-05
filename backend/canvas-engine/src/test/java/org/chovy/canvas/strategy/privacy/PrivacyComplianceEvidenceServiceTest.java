package org.chovy.canvas.strategy.privacy;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PrivacyComplianceEvidenceServiceTest {

    @Test
    void migrationCreatesPrivacyEvidenceGate() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V182__privacy_compliance_evidence.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS privacy_compliance_evidence")
                .contains("capability_key VARCHAR(128) NOT NULL")
                .contains("regulation_profile VARCHAR(128) NOT NULL")
                .contains("affected_data_classes TEXT NOT NULL")
                .contains("audit_artifact_notes TEXT NOT NULL")
                .contains("threat_model_notes TEXT NOT NULL")
                .contains("proof_command VARCHAR(1000) NOT NULL")
                .contains("rollback_note VARCHAR(1000) NOT NULL")
                .contains("decision_status VARCHAR(32) NOT NULL");
    }

    @Test
    void registerRejectsMissingDataAuditThreatOrRollbackEvidence() {
        PrivacyComplianceEvidenceService.EvidenceRepository repository =
                mock(PrivacyComplianceEvidenceService.EvidenceRepository.class);
        PrivacyComplianceEvidenceService service = new PrivacyComplianceEvidenceService(repository);

        assertThatThrownBy(() -> service.register(new PrivacyComplianceEvidenceService.EvidenceRequest(
                "dsr-export", "owner-1", "GDPR", "", "export audit bundle",
                "EU residency", "abuse and replay reviewed", "mvn test", "hide compliance action")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("affected data classes are required");

        assertThatThrownBy(() -> service.register(new PrivacyComplianceEvidenceService.EvidenceRequest(
                "federated-learning", "owner-1", "PIPL", "profile attributes",
                "model training audit", "China residency review", "", "mvn test", "disable experiment")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("threat model notes are required");
    }

    @Test
    void registerStoresBlockedDecisionUntilComplianceReview() {
        PrivacyComplianceEvidenceService.EvidenceRepository repository =
                mock(PrivacyComplianceEvidenceService.EvidenceRepository.class);
        PrivacyComplianceEvidenceService service = new PrivacyComplianceEvidenceService(repository);

        service.register(new PrivacyComplianceEvidenceService.EvidenceRequest(
                "dsr-delete", "owner-1", "GDPR", "cdp user, event log, execution trace",
                "deletion audit bundle required", "EU residency review", "identity and replay threat reviewed",
                "cd backend && mvn -pl canvas-engine test", "disable privacy_compliance.registry.enabled"));

        verify(repository).insert(argThat(record ->
                record.capabilityKey().equals("dsr-delete")
                        && record.decisionStatus().equals("BLOCKED_PENDING_REVIEW")
                        && record.auditArtifactNotes().contains("audit bundle")));
    }

    @Test
    void approvalRequiresReviewerAndNamedChildSpec() {
        PrivacyComplianceEvidenceService.EvidenceRepository repository =
                mock(PrivacyComplianceEvidenceService.EvidenceRepository.class);
        PrivacyComplianceEvidenceService service = new PrivacyComplianceEvidenceService(repository);

        assertThatThrownBy(() -> service.approve(
                "dsr-delete", "", "docs/product-evolution/specs/p3-010a-dsr-delete.md"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reviewer is required");

        assertThatThrownBy(() -> service.approve("dsr-delete", "compliance-1", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("child spec is required");

        service.approve("dsr-delete", "compliance-1", "docs/product-evolution/specs/p3-010a-dsr-delete.md");

        verify(repository).approve("dsr-delete", "compliance-1",
                "docs/product-evolution/specs/p3-010a-dsr-delete.md");
    }
}
