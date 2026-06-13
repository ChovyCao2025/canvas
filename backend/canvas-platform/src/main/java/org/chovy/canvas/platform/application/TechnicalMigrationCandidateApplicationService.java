package org.chovy.canvas.platform.application;

import org.chovy.canvas.platform.api.PlatformActor;
import org.chovy.canvas.platform.api.TechnicalMigrationCandidateFacade;
import org.chovy.canvas.platform.api.TechnicalMigrationEvidenceRequest;
import org.chovy.canvas.platform.api.TechnicalMigrationEvidenceView;
import org.chovy.canvas.platform.domain.TechnicalMigrationCandidateEvidence;
import org.chovy.canvas.platform.domain.TechnicalMigrationCandidateEvidenceRepository;
import org.chovy.canvas.platform.domain.TechnicalMigrationDecisionStatus;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Objects;

@Service
public class TechnicalMigrationCandidateApplicationService implements TechnicalMigrationCandidateFacade {

    private final TechnicalMigrationCandidateEvidenceRepository repository;

    public TechnicalMigrationCandidateApplicationService(TechnicalMigrationCandidateEvidenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public TechnicalMigrationEvidenceView register(
            PlatformActor actor,
            TechnicalMigrationEvidenceRequest request) {
        Long tenantId = requireTenantId(actor);
        String candidateKey = normalizeCandidateKey(request.candidateKey());
        requireText(request.proofCommand(), "proof command is required");
        requireText(request.baselineResultJson(), "baseline result is required");
        requireText(request.rollbackCommand(), "rollback command is required");

        TechnicalMigrationCandidateEvidence record = new TechnicalMigrationCandidateEvidence(
                tenantId,
                candidateKey,
                request.proofCommand().trim(),
                request.baselineResultJson().trim(),
                request.rollbackCommand().trim(),
                TechnicalMigrationDecisionStatus.BLOCKED_PENDING_REVIEW,
                submittedBy(actor));
        repository.insert(record);
        return toView(record);
    }

    @Override
    public boolean canStartMigration(PlatformActor actor, String candidateKey) {
        Long tenantId = requireTenantId(actor);
        TechnicalMigrationCandidateEvidence latest =
                repository.latest(tenantId, normalizeCandidateKey(candidateKey));
        return latest != null
                && latest.decisionStatus() == TechnicalMigrationDecisionStatus.APPROVED_FOR_CHILD_SPEC;
    }

    private static TechnicalMigrationEvidenceView toView(TechnicalMigrationCandidateEvidence record) {
        return new TechnicalMigrationEvidenceView(
                record.tenantId(),
                record.candidateKey(),
                record.proofCommand(),
                record.baselineResultJson(),
                record.rollbackCommand(),
                record.decisionStatus().name(),
                record.submittedBy());
    }

    private static Long requireTenantId(PlatformActor actor) {
        if (actor == null || actor.tenantId() == null) {
            throw new SecurityException("AUTH_003: missing tenant context");
        }
        return actor.tenantId();
    }

    private static String submittedBy(PlatformActor actor) {
        if (actor.username() == null || actor.username().isBlank()) {
            return "unknown";
        }
        return actor.username().trim();
    }

    private static String normalizeCandidateKey(String candidateKey) {
        requireText(candidateKey, "candidate key is required");
        String normalized = Objects.requireNonNull(candidateKey).trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9][a-z0-9-]{0,127}")) {
            throw new IllegalArgumentException("invalid candidate key: " + candidateKey);
        }
        return normalized;
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
