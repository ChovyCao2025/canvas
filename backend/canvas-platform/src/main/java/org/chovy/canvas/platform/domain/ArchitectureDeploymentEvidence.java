package org.chovy.canvas.platform.domain;

public record ArchitectureDeploymentEvidence(
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
