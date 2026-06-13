package org.chovy.canvas.platform.api;

public record ArchitectureDeploymentEvidenceRequest(
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
