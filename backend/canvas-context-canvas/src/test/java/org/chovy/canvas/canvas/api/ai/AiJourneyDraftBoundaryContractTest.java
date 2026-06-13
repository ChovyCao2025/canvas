package org.chovy.canvas.canvas.api.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class AiJourneyDraftBoundaryContractTest {

    @Test
    void proposalCarriesDraftOnlyAndDoesNotExposePublishedCanvasMutationFields() {
        AiJourneyDraftProposal proposal = new AiJourneyDraftProposal(
                8L,
                "proposal-1",
                "welcome new users",
                "apiVersion: canvas/v1",
                List.of(new AiJourneyDraftProposal.RiskFinding("MISSING_APPROVAL", "High coupon needs approval")),
                List.of(new AiJourneyDraftProposal.TraceReference("exec-1", "node-1")),
                Instant.parse("2026-06-10T01:00:00Z"));

        assertThat(proposal.dslDraft()).contains("canvas/v1");
        assertThat(proposal.riskFindings()).extracting(AiJourneyDraftProposal.RiskFinding::code)
                .containsExactly("MISSING_APPROVAL");
        assertThat(List.of(AiJourneyDraftProposal.class.getRecordComponents()))
                .extracting(RecordComponent::getName)
                .doesNotContain("publishedCanvasId", "publishedVersionId", "publish");
        assertThatThrownBy(() -> proposal.riskFindings().add(
                new AiJourneyDraftProposal.RiskFinding("OTHER", "Other")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
