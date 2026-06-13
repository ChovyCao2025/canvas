package org.chovy.canvas.platform.api;

public record ArchitectureDeploymentEvidenceView(
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
