package org.chovy.canvas.strategy.globalization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RegionalExpansionEvidenceServiceTest {

    @Test
    void migrationCreatesRegionalEvidenceGate() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V181__regional_expansion_evidence.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS regional_expansion_evidence")
                .contains("region_code VARCHAR(32) NOT NULL")
                .contains("demand_evidence TEXT NOT NULL")
                .contains("compliance_notes TEXT NOT NULL")
                .contains("data_residency_notes TEXT NOT NULL")
                .contains("proof_command VARCHAR(1000) NOT NULL")
                .contains("rollback_note VARCHAR(1000) NOT NULL")
                .contains("decision_status VARCHAR(32) NOT NULL");
    }

    @Test
    void registerRejectsMissingDemandComplianceResidencyOrRollbackEvidence() {
        RegionalExpansionEvidenceService.EvidenceRepository repository =
                mock(RegionalExpansionEvidenceService.EvidenceRepository.class);
        RegionalExpansionEvidenceService service = new RegionalExpansionEvidenceService(repository);

        assertThatThrownBy(() -> service.register(new RegionalExpansionEvidenceService.EvidenceRequest(
                "JP", "owner-1", "", "JPY and ja-JP", "Asia/Tokyo", "LINE pending",
                "APPI review", "Tokyo residency review", "pilot hypothesis",
                "mvn test", "disable region flag")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("demand evidence is required");

        assertThatThrownBy(() -> service.register(new RegionalExpansionEvidenceService.EvidenceRequest(
                "JP", "owner-1", "two enterprise prospects", "JPY and ja-JP", "Asia/Tokyo", "LINE pending",
                "APPI review", "", "pilot hypothesis", "mvn test", "disable region flag")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("data residency notes are required");
    }

    @Test
    void registerStoresBlockedDecisionUntilReviewed() {
        RegionalExpansionEvidenceService.EvidenceRepository repository =
                mock(RegionalExpansionEvidenceService.EvidenceRepository.class);
        RegionalExpansionEvidenceService service = new RegionalExpansionEvidenceService(repository);

        service.register(new RegionalExpansionEvidenceService.EvidenceRequest(
                "JP", "owner-1", "two enterprise prospects", "JPY and ja-JP",
                "Asia/Tokyo", "LINE pending", "APPI review", "Tokyo residency review",
                "pilot limited to sandbox tenants", "cd backend && mvn -pl canvas-engine test",
                "disable regional_expansion.registry.enabled"));

        verify(repository).insert(argThat(record ->
                record.regionCode().equals("JP")
                        && record.decisionStatus().equals("BLOCKED_PENDING_REVIEW")
                        && record.rollbackNote().contains("disable regional_expansion")));
    }

    @Test
    void approvalRequiresReviewerAndChildSpec() {
        RegionalExpansionEvidenceService.EvidenceRepository repository =
                mock(RegionalExpansionEvidenceService.EvidenceRepository.class);
        RegionalExpansionEvidenceService service = new RegionalExpansionEvidenceService(repository);

        assertThatThrownBy(() -> service.approve("JP", "", "docs/product-evolution/specs/p3-009a-japan-pilot.md"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reviewer is required");

        assertThatThrownBy(() -> service.approve("JP", "architect-1", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("child spec is required");

        service.approve("JP", "architect-1", "docs/product-evolution/specs/p3-009a-japan-pilot.md");

        verify(repository).approve("JP", "architect-1", "docs/product-evolution/specs/p3-009a-japan-pilot.md");
    }
}
