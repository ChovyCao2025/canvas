package org.chovy.canvas.strategy.privacy;

public class PrivacyComplianceEvidenceService {

    private final EvidenceRepository repository;

    public PrivacyComplianceEvidenceService(EvidenceRepository repository) {
        this.repository = repository;
    }

    public EvidenceRecord register(EvidenceRequest request) {
        requireText(request.capabilityKey(), "capability key is required");
        requireText(request.ownerId(), "owner is required");
        requireText(request.regulationProfile(), "regulation profile is required");
        requireText(request.affectedDataClasses(), "affected data classes are required");
        requireText(request.auditArtifactNotes(), "audit artifact notes are required");
        requireText(request.residencyImpactNotes(), "residency impact notes are required");
        requireText(request.threatModelNotes(), "threat model notes are required");
        requireText(request.proofCommand(), "proof command is required");
        requireText(request.rollbackNote(), "rollback note is required");

        EvidenceRecord record = new EvidenceRecord(
                request.capabilityKey(), request.ownerId(), request.regulationProfile(),
                request.affectedDataClasses(), request.auditArtifactNotes(),
                request.residencyImpactNotes(), request.threatModelNotes(),
                request.proofCommand(), request.rollbackNote(), "BLOCKED_PENDING_REVIEW");
        repository.insert(record);
        return record;
    }

    public void approve(String capabilityKey, String reviewerId, String childSpec) {
        requireText(capabilityKey, "capability key is required");
        requireText(reviewerId, "reviewer is required");
        requireText(childSpec, "child spec is required");
        repository.approve(capabilityKey, reviewerId, childSpec);
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    public record EvidenceRequest(
            String capabilityKey,
            String ownerId,
            String regulationProfile,
            String affectedDataClasses,
            String auditArtifactNotes,
            String residencyImpactNotes,
            String threatModelNotes,
            String proofCommand,
            String rollbackNote) {
    }

    public record EvidenceRecord(
            String capabilityKey,
            String ownerId,
            String regulationProfile,
            String affectedDataClasses,
            String auditArtifactNotes,
            String residencyImpactNotes,
            String threatModelNotes,
            String proofCommand,
            String rollbackNote,
            String decisionStatus) {
    }

    public interface EvidenceRepository {
        void insert(EvidenceRecord record);

        void approve(String capabilityKey, String reviewerId, String childSpec);
    }
}
