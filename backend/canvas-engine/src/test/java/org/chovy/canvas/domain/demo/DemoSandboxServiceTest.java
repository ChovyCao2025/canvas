package org.chovy.canvas.domain.demo;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoSandboxServiceTest {

    @Test
    void migrationCreatesSandboxLifecycleTable() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V269__sandbox_demo_sales_enablement.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS demo_sandbox")
                .contains("tenant_id BIGINT NOT NULL")
                .contains("demo_marker VARCHAR(128) NOT NULL")
                .contains("expires_at DATETIME NOT NULL")
                .contains("last_reset_at DATETIME NULL")
                .contains("last_reset_by VARCHAR(128) NULL")
                .contains("UNIQUE KEY uk_demo_sandbox_tenant");
    }

    @Test
    void installCreatesSandboxWithDemoMarkerAndExpiry() {
        DemoSandboxService.SandboxRepository repository = mock(DemoSandboxService.SandboxRepository.class);
        DemoSandboxService service = new DemoSandboxService(repository, fixedClock());

        DemoSandboxService.Sandbox sandbox = service.install(8L, "Retail Lifecycle Demo", 14);

        assertThat(sandbox.demoMarker()).isEqualTo("DEMO_TENANT_8");
        assertThat(sandbox.expiresAt()).isEqualTo(Instant.parse("2026-06-17T00:00:00Z"));
        verify(repository).upsert(argThat(saved ->
                saved.tenantId().equals(8L)
                        && saved.demoName().equals("Retail Lifecycle Demo")
                        && saved.demoMarker().equals("DEMO_TENANT_8")
                        && saved.status().equals("ACTIVE")));
    }

    @Test
    void resetRequiresExistingSandboxAndRecordsResetCommand() {
        DemoSandboxService.SandboxRepository repository = mock(DemoSandboxService.SandboxRepository.class);
        DemoSandboxService service = new DemoSandboxService(repository, fixedClock());
        when(repository.get(8L)).thenReturn(new DemoSandboxService.Sandbox(
                8L, "Retail Lifecycle Demo", "DEMO_TENANT_8", "ACTIVE",
                Instant.parse("2026-06-17T00:00:00Z"), null));

        DemoSandboxService.ResetResult result = service.reset(8L, "operator-1");

        assertThat(result.demoMarker()).isEqualTo("DEMO_TENANT_8");
        assertThat(result.resetAt()).isEqualTo(Instant.parse("2026-06-03T00:00:00Z"));
        verify(repository).recordReset(8L, "operator-1", Instant.parse("2026-06-03T00:00:00Z"));
    }

    @Test
    void resetRejectsMissingSandbox() {
        DemoSandboxService.SandboxRepository repository = mock(DemoSandboxService.SandboxRepository.class);
        DemoSandboxService service = new DemoSandboxService(repository, fixedClock());
        when(repository.get(8L)).thenReturn(null);

        assertThatThrownBy(() -> service.reset(8L, "operator-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sandbox tenant 8 is not installed");
    }

    @Test
    void expiredSandboxesAreListedForCleanup() {
        DemoSandboxService.SandboxRepository repository = mock(DemoSandboxService.SandboxRepository.class);
        DemoSandboxService service = new DemoSandboxService(repository, fixedClock());
        when(repository.findExpired(Instant.parse("2026-06-03T00:00:00Z"))).thenReturn(List.of(
                new DemoSandboxService.Sandbox(
                        9L, "Old Demo", "DEMO_TENANT_9", "EXPIRED",
                        Instant.parse("2026-06-01T00:00:00Z"), null)));

        assertThat(service.expired()).extracting(DemoSandboxService.Sandbox::tenantId).containsExactly(9L);
    }

    @Test
    void installRejectsUnsafeInputs() {
        DemoSandboxService.SandboxRepository repository = mock(DemoSandboxService.SandboxRepository.class);
        DemoSandboxService service = new DemoSandboxService(repository, fixedClock());

        assertThatThrownBy(() -> service.install(null, "Retail Demo", 14))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenant id is required");
        assertThatThrownBy(() -> service.install(8L, "", 14))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("demo name is required");
        assertThatThrownBy(() -> service.install(8L, "Retail Demo", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ttlDays must be between 1 and 90");
    }

    private Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-06-03T00:00:00Z"), ZoneOffset.UTC);
    }
}
