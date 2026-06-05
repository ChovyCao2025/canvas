package org.chovy.canvas.strategy.growth;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProductLedGrowthEvidenceServiceTest {

    @Test
    void migrationCreatesProductLedGrowthEvidenceGate() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V184__product_led_growth_evidence.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS product_led_growth_evidence")
                .contains("opportunity_key VARCHAR(128) NOT NULL")
                .contains("funnel_stage VARCHAR(64) NOT NULL")
                .contains("activation_metric VARCHAR(255) NOT NULL")
                .contains("consent_requirement TEXT NOT NULL")
                .contains("content_risk_notes TEXT NOT NULL")
                .contains("proof_command VARCHAR(1000) NOT NULL")
                .contains("rollback_note VARCHAR(1000) NOT NULL")
                .contains("decision_status VARCHAR(32) NOT NULL");
    }

    @Test
    void registerRejectsMissingMetricConsentRiskOrRollback() {
        ProductLedGrowthEvidenceService.EvidenceRepository repository =
                mock(ProductLedGrowthEvidenceService.EvidenceRepository.class);
        ProductLedGrowthEvidenceService service = new ProductLedGrowthEvidenceService(repository);

        assertThatThrownBy(() -> service.register(new ProductLedGrowthEvidenceService.EvidenceRequest(
                "public-template-gallery", "growth-1", "activation", "marketing operator",
                "", "explicit publisher consent", "public content review",
                "increase activated workspaces", "npm test", "hide gallery route")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("activation metric is required");

        assertThatThrownBy(() -> service.register(new ProductLedGrowthEvidenceService.EvidenceRequest(
                "customer-story-loop", "growth-1", "expansion", "customer success",
                "case study opt-in rate", "", "public content review",
                "increase qualified references", "npm test", "hide story route")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consent requirement is required");
    }

    @Test
    void registerStoresBlockedDecisionUntilGrowthReview() {
        ProductLedGrowthEvidenceService.EvidenceRepository repository =
                mock(ProductLedGrowthEvidenceService.EvidenceRepository.class);
        ProductLedGrowthEvidenceService service = new ProductLedGrowthEvidenceService(repository);

        service.register(new ProductLedGrowthEvidenceService.EvidenceRequest(
                "referral-invite", "growth-1", "activation", "workspace admin",
                "invited workspace activation rate", "recipient consent and anti-spam review",
                "low risk with rate limits", "increase activated referred workspaces",
                "cd frontend && npm test -- --run", "disable product_led_growth.registry.enabled"));

        verify(repository).insert(argThat(record ->
                record.opportunityKey().equals("referral-invite")
                        && record.decisionStatus().equals("BLOCKED_PENDING_REVIEW")
                        && record.activationMetric().contains("activation rate")));
    }

    @Test
    void approvalRequiresReviewerAndChildSpec() {
        ProductLedGrowthEvidenceService.EvidenceRepository repository =
                mock(ProductLedGrowthEvidenceService.EvidenceRepository.class);
        ProductLedGrowthEvidenceService service = new ProductLedGrowthEvidenceService(repository);

        assertThatThrownBy(() -> service.approve(
                "referral-invite", "", "docs/product-evolution/specs/p3-012a-referral-invite.md"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reviewer is required");

        assertThatThrownBy(() -> service.approve("referral-invite", "growth-lead-1", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("child spec is required");

        service.approve("referral-invite", "growth-lead-1", "docs/product-evolution/specs/p3-012a-referral-invite.md");

        verify(repository).approve("referral-invite", "growth-lead-1",
                "docs/product-evolution/specs/p3-012a-referral-invite.md");
    }
}
