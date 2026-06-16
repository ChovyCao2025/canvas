package org.chovy.canvas.canvas.api.ai;

import java.time.Instant;
import java.util.List;

/**
 * 承载AiJourneyDraftProposal的数据快照。
 */
public record AiJourneyDraftProposal(
        /**
         * 记录租户标识。
         */
        Long tenantId,
        /**
         * 记录proposal标识。
         */
        String proposalId,
        /**
         * 记录sourcePrompt。
         */
        String sourcePrompt,
        /**
         * 记录dslDraft。
         */
        String dslDraft,
        /**
         * 记录riskFindings。
         */
        List<RiskFinding> riskFindings,
        /**
         * 记录traceReferences。
         */
        List<TraceReference> traceReferences,
        /**
         * 记录创建时间。
         */
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

    /**
     * 承载RiskFinding的数据快照。
     */
    public record RiskFinding(String code, String message) {
        public RiskFinding {
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("code is required");
            }
            message = message == null ? "" : message;
        }
    }

    /**
     * 承载TraceReference的数据快照。
     */
    public record TraceReference(String executionId, String nodeId) {
        public TraceReference {
            if (executionId == null || executionId.isBlank()) {
                throw new IllegalArgumentException("executionId is required");
            }
            nodeId = nodeId == null ? "" : nodeId;
        }
    }
}
