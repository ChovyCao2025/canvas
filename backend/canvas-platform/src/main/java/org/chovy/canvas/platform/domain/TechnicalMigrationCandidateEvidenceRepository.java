package org.chovy.canvas.platform.domain;

public interface TechnicalMigrationCandidateEvidenceRepository {

    void insert(TechnicalMigrationCandidateEvidence record);

    TechnicalMigrationCandidateEvidence latest(Long tenantId, String candidateKey);
}
