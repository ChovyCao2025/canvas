package org.chovy.canvas.platform.domain;

public interface ArchitectureDeploymentEvidenceRepository {

    void insert(ArchitectureDeploymentEvidence record);

    void approve(String candidateKey, String reviewerId, String childSpec);
}
