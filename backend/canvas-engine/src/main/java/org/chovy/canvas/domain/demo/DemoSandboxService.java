package org.chovy.canvas.domain.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class DemoSandboxService {

    private static final int MAX_TTL_DAYS = 90;

    private final SandboxRepository repository;
    private final Clock clock;

    @Autowired
    public DemoSandboxService(SandboxRepository repository) {
        this(repository, Clock.systemUTC());
    }

    public DemoSandboxService(SandboxRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Sandbox install(Long tenantId, String demoName, int ttlDays) {
        requireTenantId(tenantId);
        requireText(demoName, "demo name is required");
        if (ttlDays < 1 || ttlDays > MAX_TTL_DAYS) {
            throw new IllegalArgumentException("ttlDays must be between 1 and 90");
        }
        Sandbox sandbox = new Sandbox(
                tenantId,
                demoName.trim(),
                "DEMO_TENANT_" + tenantId,
                "ACTIVE",
                clock.instant().plus(ttlDays, ChronoUnit.DAYS),
                null);
        repository.upsert(sandbox);
        return sandbox;
    }

    public ResetResult reset(Long tenantId, String operator) {
        requireTenantId(tenantId);
        requireText(operator, "operator is required");
        Sandbox sandbox = repository.get(tenantId);
        if (sandbox == null) {
            throw new IllegalStateException("sandbox tenant " + tenantId + " is not installed");
        }
        Instant resetAt = clock.instant();
        repository.recordReset(tenantId, operator.trim(), resetAt);
        return new ResetResult(tenantId, sandbox.demoMarker(), resetAt);
    }

    public List<Sandbox> expired() {
        return repository.findExpired(clock.instant());
    }

    private static void requireTenantId(Long tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenant id is required");
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    public record Sandbox(
            Long tenantId,
            String demoName,
            String demoMarker,
            String status,
            Instant expiresAt,
            Instant lastResetAt) {
    }

    public record ResetResult(Long tenantId, String demoMarker, Instant resetAt) {
    }

    public interface SandboxRepository {
        void upsert(Sandbox sandbox);

        Sandbox get(Long tenantId);

        void recordReset(Long tenantId, String operator, Instant resetAt);

        List<Sandbox> findExpired(Instant now);
    }
}
