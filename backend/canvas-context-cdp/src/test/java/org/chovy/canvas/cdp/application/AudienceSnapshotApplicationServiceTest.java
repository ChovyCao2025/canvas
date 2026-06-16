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

/**
 * 验证 AudienceSnapshotApplicationService 的核心行为。
 */
class AudienceSnapshotApplicationServiceTest {

    /**
     * 执行 fixed 对应的 CDP 业务操作。
     */
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T02:00:00Z"),
            /**
             * 执行 of 对应的 CDP 业务操作。
             */
            ZoneId.of("Asia/Shanghai"));

    /**
     * 执行 lockSnapshotFiltersBlankUsersDeduplicatesAndPersistsStaticSnapshot 对应的 CDP 业务操作。
     */
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

    /**
     * 执行 lockSnapshotEnforcesConfiguredMaximum 对应的 CDP 业务操作。
     */
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

    /**
     * 返回默认的Mode Normalizes Missing And Unknown Values To Static Locked。
     */
    @Test
    void defaultModeNormalizesMissingAndUnknownValuesToStaticLocked() {
        FakeAudienceRepository repository = new FakeAudienceRepository();
        repository.defaultModes.put(100L, "dynamic_refresh");
        AudienceSnapshotApplicationService service = new AudienceSnapshotApplicationService(repository, CLOCK, 10);

        assertThat(service.defaultModeForAudience(100L)).isEqualTo("DYNAMIC_REFRESH");
        assertThat(service.defaultModeForAudience(200L)).isEqualTo("STATIC_LOCKED");
        assertThat(AudienceSnapshotMode.normalize("invalid")).isEqualTo(AudienceSnapshotMode.STATIC_LOCKED);
    }

    /**
     * 定义 FakeAudience 的持久化访问契约。
     */
    private static final class FakeAudienceRepository implements AudienceSnapshotRepository {
        private final Map<Long, List<String>> resolvedUsers = new LinkedHashMap<>();
        private final Map<Long, AudienceSnapshot> snapshots = new LinkedHashMap<>();
        private final Map<Long, String> defaultModes = new LinkedHashMap<>();

        /**
         * 执行 resolveUsers 对应的 CDP 业务操作。
         */
        @Override
        public List<String> resolveUsers(Long audienceId) {
            return resolvedUsers.getOrDefault(audienceId, List.of());
        }

        /**
         * 保存save。
         */
        @Override
        public AudienceSnapshot save(AudienceSnapshot snapshot) {
            AudienceSnapshot saved = snapshot.withId(500L);
            snapshots.put(saved.id(), saved);
            return saved;
        }

        /**
         * 查找Snapshot。
         */
        @Override
        public AudienceSnapshot findSnapshot(Long snapshotId) {
            return snapshots.get(snapshotId);
        }

        /**
         * 返回默认的Snapshot Mode。
         */
        @Override
        public String defaultSnapshotMode(Long audienceId) {
            return defaultModes.get(audienceId);
        }
    }
}
