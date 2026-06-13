package org.chovy.canvas.platform.domain;

public record TechnicalMigrationCandidateEvidence(
        Long tenantId,
        String candidateKey,
        String proofCommand,
        String baselineResultJson,
        String rollbackCommand,
        TechnicalMigrationDecisionStatus decisionStatus,
        String submittedBy) {
}
