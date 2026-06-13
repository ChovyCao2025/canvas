package org.chovy.canvas.platform.api;

public record TechnicalMigrationEvidenceRequest(
        String candidateKey,
        String proofCommand,
        String baselineResultJson,
        String rollbackCommand) {
}
