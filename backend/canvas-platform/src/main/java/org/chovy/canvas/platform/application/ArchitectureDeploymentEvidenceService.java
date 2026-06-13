package org.chovy.canvas.platform.application;

import org.chovy.canvas.platform.api.ArchitectureDeploymentEvidenceFacade;
import org.chovy.canvas.platform.api.ArchitectureDeploymentEvidenceRequest;
import org.chovy.canvas.platform.api.ArchitectureDeploymentEvidenceView;
import org.chovy.canvas.platform.domain.ArchitectureDeploymentEvidence;
import org.chovy.canvas.platform.domain.ArchitectureDeploymentEvidenceRepository;
import org.springframework.stereotype.Service;

@Service
public class ArchitectureDeploymentEvidenceService implements ArchitectureDeploymentEvidenceFacade {

    private static final String BLOCKED_PENDING_REVIEW = "BLOCKED_PENDING_REVIEW";

    private final ArchitectureDeploymentEvidenceRepository repository;

    public ArchitectureDeploymentEvidenceService(ArchitectureDeploymentEvidenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public ArchitectureDeploymentEvidenceView register(ArchitectureDeploymentEvidenceRequest request) {
        requireText(request.candidateKey(), "candidate key is required");
        requireText(request.ownerId(), "owner is required");
        requireText(request.currentStateEvidence(), "current-state evidence is required");
        requireText(request.targetArchitecture(), "target architecture is required");
        requireText(request.scalingTrigger(), "scaling trigger is required");
        requireText(request.operationalCostNotes(), "operational cost notes are required");
        requireText(request.dependencyNotes(), "dependency notes are required");
        requireText(request.proofCommand(), "proof command is required");
        requireText(request.rollbackPlan(), "rollback plan is required");

        ArchitectureDeploymentEvidence record = new ArchitectureDeploymentEvidence(
                request.candidateKey().trim(),
                request.ownerId().trim(),
                request.currentStateEvidence().trim(),
                request.targetArchitecture().trim(),
                request.scalingTrigger().trim(),
                request.operationalCostNotes().trim(),
                request.dependencyNotes().trim(),
                request.proofCommand().trim(),
                request.rollbackPlan().trim(),
                BLOCKED_PENDING_REVIEW);
        repository.insert(record);
        return toView(record);
    }

    @Override
    public void approve(String candidateKey, String reviewerId, String childSpec) {
        requireText(candidateKey, "candidate key is required");
        requireText(reviewerId, "reviewer is required");
        requireText(childSpec, "child spec is required");
        repository.approve(candidateKey.trim(), reviewerId.trim(), childSpec.trim());
    }

    private static ArchitectureDeploymentEvidenceView toView(ArchitectureDeploymentEvidence record) {
        return new ArchitectureDeploymentEvidenceView(
                record.candidateKey(),
                record.ownerId(),
                record.currentStateEvidence(),
                record.targetArchitecture(),
                record.scalingTrigger(),
                record.operationalCostNotes(),
                record.dependencyNotes(),
                record.proofCommand(),
                record.rollbackPlan(),
                record.decisionStatus());
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
