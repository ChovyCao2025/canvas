package org.chovy.canvas.platform.api;

public interface TechnicalMigrationCandidateFacade {

    TechnicalMigrationEvidenceView register(PlatformActor actor, TechnicalMigrationEvidenceRequest request);

    boolean canStartMigration(PlatformActor actor, String candidateKey);
}
