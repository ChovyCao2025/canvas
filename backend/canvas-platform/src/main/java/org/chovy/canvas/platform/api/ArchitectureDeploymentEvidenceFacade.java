package org.chovy.canvas.platform.api;

public interface ArchitectureDeploymentEvidenceFacade {

    ArchitectureDeploymentEvidenceView register(ArchitectureDeploymentEvidenceRequest request);

    void approve(String candidateKey, String reviewerId, String childSpec);
}
