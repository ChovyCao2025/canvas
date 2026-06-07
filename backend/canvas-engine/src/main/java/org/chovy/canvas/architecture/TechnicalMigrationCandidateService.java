package org.chovy.canvas.architecture;

import org.chovy.canvas.common.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Objects;

@Service
public class TechnicalMigrationCandidateService {

    public static final String BLOCKED_PENDING_REVIEW = "BLOCKED_PENDING_REVIEW";
    public static final String APPROVED_FOR_CHILD_SPEC = "APPROVED_FOR_CHILD_SPEC";

    private final EvidenceRepository repository;

    public TechnicalMigrationCandidateService(EvidenceRepository repository) {
        this.repository = repository;
    }

    public TechnicalMigrationCandidateEvidenceRecord register(TenantContext tenant, EvidenceRequest request) {
        Long tenantId = requireTenantId(tenant);
        String candidateKey = normalizeCandidateKey(request.candidateKey());
        requireText(request.proofCommand(), "proof command is required");
        requireText(request.baselineResultJson(), "baseline result is required");
        requireText(request.rollbackCommand(), "rollback command is required");

        TechnicalMigrationCandidateEvidenceRecord record = new TechnicalMigrationCandidateEvidenceRecord(
                tenantId,
                candidateKey,
                request.proofCommand().trim(),
                request.baselineResultJson().trim(),
                request.rollbackCommand().trim(),
                BLOCKED_PENDING_REVIEW,
                submittedBy(tenant));
        repository.insert(record);
        return record;
    }

    public boolean canStartMigration(TenantContext tenant, String candidateKey) {
        Long tenantId = requireTenantId(tenant);
        TechnicalMigrationCandidateEvidenceRecord latest = repository.latest(tenantId, normalizeCandidateKey(candidateKey));
        return latest != null && APPROVED_FOR_CHILD_SPEC.equals(latest.decisionStatus());
    }

    private static Long requireTenantId(TenantContext tenant) {
        if (tenant == null || tenant.tenantId() == null) {
            throw new SecurityException("AUTH_003: missing tenant context");
        }
        return tenant.tenantId();
    }

    private static String submittedBy(TenantContext tenant) {
        if (tenant.username() == null || tenant.username().isBlank()) {
            return "unknown";
        }
        return tenant.username().trim();
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

    public record EvidenceRequest(
            String candidateKey,
            String proofCommand,
            String baselineResultJson,
            String rollbackCommand,
            String submittedBy) {
    }

    public interface EvidenceRepository {
        void insert(TechnicalMigrationCandidateEvidenceRecord record);

        TechnicalMigrationCandidateEvidenceRecord latest(Long tenantId, String candidateKey);
    }
}
