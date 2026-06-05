package org.chovy.canvas.strategy.growth;

public class ProductLedGrowthEvidenceService {

    private final EvidenceRepository repository;

    public ProductLedGrowthEvidenceService(EvidenceRepository repository) {
        this.repository = repository;
    }

    public EvidenceRecord register(EvidenceRequest request) {
        requireText(request.opportunityKey(), "opportunity key is required");
        requireText(request.ownerId(), "owner is required");
        requireText(request.funnelStage(), "funnel stage is required");
        requireText(request.targetPersona(), "target persona is required");
        requireText(request.activationMetric(), "activation metric is required");
        requireText(request.consentRequirement(), "consent requirement is required");
        requireText(request.contentRiskNotes(), "content risk notes are required");
        requireText(request.experimentHypothesis(), "experiment hypothesis is required");
        requireText(request.proofCommand(), "proof command is required");
        requireText(request.rollbackNote(), "rollback note is required");

        EvidenceRecord record = new EvidenceRecord(
                request.opportunityKey(), request.ownerId(), request.funnelStage(),
                request.targetPersona(), request.activationMetric(), request.consentRequirement(),
                request.contentRiskNotes(), request.experimentHypothesis(), request.proofCommand(),
                request.rollbackNote(), "BLOCKED_PENDING_REVIEW");
        repository.insert(record);
        return record;
    }

    public void approve(String opportunityKey, String reviewerId, String childSpec) {
        requireText(opportunityKey, "opportunity key is required");
        requireText(reviewerId, "reviewer is required");
        requireText(childSpec, "child spec is required");
        repository.approve(opportunityKey, reviewerId, childSpec);
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    public record EvidenceRequest(
            String opportunityKey,
            String ownerId,
            String funnelStage,
            String targetPersona,
            String activationMetric,
            String consentRequirement,
            String contentRiskNotes,
            String experimentHypothesis,
            String proofCommand,
            String rollbackNote) {
    }

    public record EvidenceRecord(
            String opportunityKey,
            String ownerId,
            String funnelStage,
            String targetPersona,
            String activationMetric,
            String consentRequirement,
            String contentRiskNotes,
            String experimentHypothesis,
            String proofCommand,
            String rollbackNote,
            String decisionStatus) {
    }

    public interface EvidenceRepository {
        void insert(EvidenceRecord record);

        void approve(String opportunityKey, String reviewerId, String childSpec);
    }
}
