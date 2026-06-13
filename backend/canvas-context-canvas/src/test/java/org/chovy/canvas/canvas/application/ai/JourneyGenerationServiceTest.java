package org.chovy.canvas.canvas.application.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.chovy.canvas.canvas.api.ai.AiJourneyDraftProposal;
import org.junit.jupiter.api.Test;

class JourneyGenerationServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-11T08:00:00Z"), ZoneOffset.UTC);

    private final JourneyGenerationService service = JourneyGenerationService.mock(CLOCK);

    @Test
    void mockProviderGeneratesValidCanvasDslDraftProposalWithoutPublishFields() {
        AiJourneyDraftProposal proposal = service.generateDraft(new JourneyGenerationService.GenerationRequest(
                7L,
                "Create a new user welcome journey with webhook trigger, condition, message, coupon, approval and end.",
                List.of(new AiJourneyDraftProposal.RiskFinding("MISSING_FREQUENCY_CAP", "Add a frequency cap")),
                List.of(new AiJourneyDraftProposal.TraceReference("exec-42", "message")),
                "operator-1"));

        assertThat(proposal.tenantId()).isEqualTo(7L);
        assertThat(proposal.proposalId()).startsWith("ai-journey-");
        assertThat(proposal.createdAt()).isEqualTo(Instant.parse("2026-06-11T08:00:00Z"));
        assertThat(proposal.sourcePrompt()).contains("welcome journey");
        assertThat(proposal.dslDraft())
                .contains("apiVersion: canvas/v1")
                .contains("kind: Journey")
                .contains("name: ai-new-user-welcome")
                .contains("type: webhook")
                .contains("type: message")
                .contains("type: end")
                .doesNotContain("publishedCanvasId", "publishedVersionId", "publish: true");
        assertThat(proposal.riskFindings())
                .extracting(AiJourneyDraftProposal.RiskFinding::code)
                .containsExactly("MISSING_FREQUENCY_CAP");
        assertThat(proposal.traceReferences())
                .extracting(AiJourneyDraftProposal.TraceReference::executionId)
                .containsExactly("exec-42");
    }

    @Test
    void mockProviderUsesDemoSafeFallbackWhenPromptIsBlank() {
        AiJourneyDraftProposal proposal = service.generateDraft(new JourneyGenerationService.GenerationRequest(
                3L,
                " ",
                null,
                null,
                null));

        assertThat(proposal.sourcePrompt()).isEmpty();
        assertThat(proposal.dslDraft())
                .contains("name: ai-marketing-journey")
                .contains("event: demo.event")
                .contains("type: risk-check");
        assertThat(proposal.riskFindings()).isEmpty();
        assertThat(proposal.traceReferences()).isEmpty();
    }
}
