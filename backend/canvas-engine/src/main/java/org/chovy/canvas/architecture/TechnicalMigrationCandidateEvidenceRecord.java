package org.chovy.canvas.architecture;

public record TechnicalMigrationCandidateEvidenceRecord(
        Long tenantId,
        String candidateKey,
        String proofCommand,
        String baselineResultJson,
        String rollbackCommand,
        String decisionStatus,
        String submittedBy) {
}
