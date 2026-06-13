package org.chovy.canvas.cdp.application;

import org.chovy.canvas.cdp.api.AudienceSnapshotLockCommand;
import org.chovy.canvas.cdp.api.AudienceSnapshotView;
import org.chovy.canvas.cdp.domain.AudienceSnapshot;
import org.chovy.canvas.cdp.domain.AudienceSnapshotMode;
import org.chovy.canvas.cdp.domain.AudienceSnapshotRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AudienceSnapshotApplicationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void lockSnapshotFiltersBlankUsersDeduplicatesAndPersistsStaticSnapshot() {
        FakeAudienceRepository repository = new FakeAudienceRepository();
        repository.resolvedUsers.put(100L, List.of("u1", "", "u2", "u1", " "));
        AudienceSnapshotApplicationService service = new AudienceSnapshotApplicationService(repository, CLOCK, 10);

        AudienceSnapshotView snapshot = service.lockSnapshot(new AudienceSnapshotLockCommand(
                100L,
                200L,
                300L,
                "node-1",
                "operator-1"));

        assertThat(snapshot.id()).isEqualTo(500L);
        assertThat(snapshot.snapshotMode()).isEqualTo("STATIC_LOCKED");
        assertThat(snapshot.userCount()).isEqualTo(2);
        assertThat(service.users(500L)).containsExactly("u1", "u2");
        assertThat(service.contains(500L, "u2")).isTrue();
        assertThat(service.contains(500L, "missing")).isFalse();
        assertThat(repository.snapshots.get(500L).createdAt()).isEqualTo(LocalDateTime.parse("2026-06-06T10:00:00"));
    }

    @Test
    void lockSnapshotEnforcesConfiguredMaximum() {
        FakeAudienceRepository repository = new FakeAudienceRepository();
        repository.resolvedUsers.put(100L, List.of("u1", "u2", "u3"));
        AudienceSnapshotApplicationService service = new AudienceSnapshotApplicationService(repository, CLOCK, 2);

        assertThatThrownBy(() -> service.lockSnapshot(new AudienceSnapshotLockCommand(
                100L,
                200L,
                300L,
                "node-1",
                "operator-1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AUDIENCE_SNAPSHOT_LIMIT");
    }

    @Test
    void defaultModeNormalizesMissingAndUnknownValuesToStaticLocked() {
        FakeAudienceRepository repository = new FakeAudienceRepository();
        repository.defaultModes.put(100L, "dynamic_refresh");
        AudienceSnapshotApplicationService service = new AudienceSnapshotApplicationService(repository, CLOCK, 10);

        assertThat(service.defaultModeForAudience(100L)).isEqualTo("DYNAMIC_REFRESH");
        assertThat(service.defaultModeForAudience(200L)).isEqualTo("STATIC_LOCKED");
        assertThat(AudienceSnapshotMode.normalize("invalid")).isEqualTo(AudienceSnapshotMode.STATIC_LOCKED);
    }

    private static final class FakeAudienceRepository implements AudienceSnapshotRepository {
        private final Map<Long, List<String>> resolvedUsers = new LinkedHashMap<>();
        private final Map<Long, AudienceSnapshot> snapshots = new LinkedHashMap<>();
        private final Map<Long, String> defaultModes = new LinkedHashMap<>();

        @Override
        public List<String> resolveUsers(Long audienceId) {
            return resolvedUsers.getOrDefault(audienceId, List.of());
        }

        @Override
        public AudienceSnapshot save(AudienceSnapshot snapshot) {
            AudienceSnapshot saved = snapshot.withId(500L);
            snapshots.put(saved.id(), saved);
            return saved;
        }

        @Override
        public AudienceSnapshot findSnapshot(Long snapshotId) {
            return snapshots.get(snapshotId);
        }

        @Override
        public String defaultSnapshotMode(Long audienceId) {
            return defaultModes.get(audienceId);
        }
    }
}
