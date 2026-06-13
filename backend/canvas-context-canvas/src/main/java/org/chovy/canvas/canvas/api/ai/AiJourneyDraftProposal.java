package org.chovy.canvas.canvas.api.ai;

import java.time.Instant;
import java.util.List;

public record AiJourneyDraftProposal(
        Long tenantId,
        String proposalId,
        String sourcePrompt,
        String dslDraft,
        List<RiskFinding> riskFindings,
        List<TraceReference> traceReferences,
        Instant createdAt) {

    public AiJourneyDraftProposal {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (proposalId == null || proposalId.isBlank()) {
            throw new IllegalArgumentException("proposalId is required");
        }
        sourcePrompt = sourcePrompt == null ? "" : sourcePrompt;
        if (dslDraft == null || dslDraft.isBlank()) {
            throw new IllegalArgumentException("dslDraft is required");
        }
        riskFindings = List.copyOf(riskFindings == null ? List.of() : riskFindings);
        traceReferences = List.copyOf(traceReferences == null ? List.of() : traceReferences);
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public record RiskFinding(String code, String message) {
        public RiskFinding {
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("code is required");
            }
            message = message == null ? "" : message;
        }
    }

    public record TraceReference(String executionId, String nodeId) {
        public TraceReference {
            if (executionId == null || executionId.isBlank()) {
                throw new IllegalArgumentException("executionId is required");
            }
            nodeId = nodeId == null ? "" : nodeId;
        }
    }
}
