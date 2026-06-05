package org.chovy.canvas.strategy.globalization;

public class RegionalExpansionEvidenceService {

    private final EvidenceRepository repository;

    public RegionalExpansionEvidenceService(EvidenceRepository repository) {
        this.repository = repository;
    }

    public EvidenceRecord register(EvidenceRequest request) {
        requireText(request.regionCode(), "region code is required");
        requireText(request.ownerId(), "owner is required");
        requireText(request.demandEvidence(), "demand evidence is required");
        requireText(request.complianceNotes(), "compliance notes are required");
        requireText(request.dataResidencyNotes(), "data residency notes are required");
        requireText(request.proofCommand(), "proof command is required");
        requireText(request.rollbackNote(), "rollback note is required");

        EvidenceRecord record = new EvidenceRecord(
                request.regionCode(), request.ownerId(), request.demandEvidence(),
                request.localeCurrencyNotes(), request.timezoneNotes(), request.channelNotes(),
                request.complianceNotes(), request.dataResidencyNotes(), request.rolloutHypothesis(),
                request.proofCommand(), request.rollbackNote(), "BLOCKED_PENDING_REVIEW");
        repository.insert(record);
        return record;
    }

    public void approve(String regionCode, String reviewerId, String childSpec) {
        requireText(regionCode, "region code is required");
        requireText(reviewerId, "reviewer is required");
        requireText(childSpec, "child spec is required");
        repository.approve(regionCode, reviewerId, childSpec);
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    public record EvidenceRequest(
            String regionCode,
            String ownerId,
            String demandEvidence,
            String localeCurrencyNotes,
            String timezoneNotes,
            String channelNotes,
            String complianceNotes,
            String dataResidencyNotes,
            String rolloutHypothesis,
            String proofCommand,
            String rollbackNote) {
    }

    public record EvidenceRecord(
            String regionCode,
            String ownerId,
            String demandEvidence,
            String localeCurrencyNotes,
            String timezoneNotes,
            String channelNotes,
            String complianceNotes,
            String dataResidencyNotes,
            String rolloutHypothesis,
            String proofCommand,
            String rollbackNote,
            String decisionStatus) {
    }

    public interface EvidenceRepository {
        void insert(EvidenceRecord record);

        void approve(String regionCode, String reviewerId, String childSpec);
    }
}
