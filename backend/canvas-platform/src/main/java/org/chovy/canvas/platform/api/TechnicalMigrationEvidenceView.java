package org.chovy.canvas.platform.api;

public record TechnicalMigrationEvidenceView(
        Long tenantId,
        String candidateKey,
        String proofCommand,
        String baselineResultJson,
        String rollbackCommand,
        String decisionStatus,
        String submittedBy) {
}
