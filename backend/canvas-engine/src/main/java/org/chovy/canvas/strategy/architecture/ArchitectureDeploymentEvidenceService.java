package org.chovy.canvas.strategy.architecture;

public class ArchitectureDeploymentEvidenceService {

    private final EvidenceRepository repository;

    public ArchitectureDeploymentEvidenceService(EvidenceRepository repository) {
        this.repository = repository;
    }

    public EvidenceRecord register(EvidenceRequest request) {
        requireText(request.candidateKey(), "candidate key is required");
        requireText(request.ownerId(), "owner is required");
        requireText(request.currentStateEvidence(), "current-state evidence is required");
        requireText(request.targetArchitecture(), "target architecture is required");
        requireText(request.scalingTrigger(), "scaling trigger is required");
        requireText(request.operationalCostNotes(), "operational cost notes are required");
        requireText(request.dependencyNotes(), "dependency notes are required");
        requireText(request.proofCommand(), "proof command is required");
        requireText(request.rollbackPlan(), "rollback plan is required");

        EvidenceRecord record = new EvidenceRecord(
                request.candidateKey(), request.ownerId(), request.currentStateEvidence(),
                request.targetArchitecture(), request.scalingTrigger(), request.operationalCostNotes(),
                request.dependencyNotes(), request.proofCommand(), request.rollbackPlan(),
                "BLOCKED_PENDING_REVIEW");
        repository.insert(record);
        return record;
    }

    public void approve(String candidateKey, String reviewerId, String childSpec) {
        requireText(candidateKey, "candidate key is required");
        requireText(reviewerId, "reviewer is required");
        requireText(childSpec, "child spec is required");
        repository.approve(candidateKey, reviewerId, childSpec);
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    public record EvidenceRequest(
            String candidateKey,
            String ownerId,
            String currentStateEvidence,
            String targetArchitecture,
            String scalingTrigger,
            String operationalCostNotes,
            String dependencyNotes,
            String proofCommand,
            String rollbackPlan) {
    }

    public record EvidenceRecord(
            String candidateKey,
            String ownerId,
            String currentStateEvidence,
            String targetArchitecture,
            String scalingTrigger,
            String operationalCostNotes,
            String dependencyNotes,
            String proofCommand,
            String rollbackPlan,
            String decisionStatus) {
    }

    public interface EvidenceRepository {
        void insert(EvidenceRecord record);

        void approve(String candidateKey, String reviewerId, String childSpec);
    }
}
