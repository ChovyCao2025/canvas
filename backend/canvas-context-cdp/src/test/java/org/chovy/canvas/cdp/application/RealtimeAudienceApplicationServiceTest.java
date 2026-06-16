package org.chovy.canvas.cdp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;

import org.chovy.canvas.cdp.api.RealtimeAudienceFacade;
import org.junit.jupiter.api.Test;

/**
 * 验证 RealtimeAudienceApplicationService 的核心行为。
 */
class RealtimeAudienceApplicationServiceTest {

    /**
     * 执行 processEventAddsMatchingUsersAndCanRemoveNonMatchingUsers 对应的 CDP 业务操作。
     */
    @Test
    void processEventAddsMatchingUsersAndCanRemoveNonMatchingUsers() {
        RealtimeAudienceFacade service = new RealtimeAudienceApplicationService();

        RealtimeAudienceFacade.EventResult matched = service.processEvent(7L, 100L,
                new RealtimeAudienceFacade.CdpEvent("evt-1", "user-1", Instant.parse("2026-06-14T10:00:00Z"),
                        Map.of("tier", "gold")), true);
        RealtimeAudienceFacade.EventResult removed = service.processEvent(7L, 100L,
                new RealtimeAudienceFacade.CdpEvent("evt-2", "user-1", Instant.parse("2026-06-14T10:01:00Z"),
                        Map.of("tier", "silver")), true);

        assertThat(matched)
                .returns(100L, RealtimeAudienceFacade.EventResult::audienceId)
                .returns("user-1", RealtimeAudienceFacade.EventResult::userId)
                .returns(true, RealtimeAudienceFacade.EventResult::matched)
                .returns(false, RealtimeAudienceFacade.EventResult::removed)
                .returns(2, RealtimeAudienceFacade.EventResult::memberCount);
        assertThat(removed)
                .returns(false, RealtimeAudienceFacade.EventResult::matched)
                .returns(true, RealtimeAudienceFacade.EventResult::removed)
                .returns(1, RealtimeAudienceFacade.EventResult::memberCount);
    }

    /**
     * 执行 snapshotsCaptureCurrentMembersAndRespectLimit 对应的 CDP 业务操作。
     */
    @Test
    void snapshotsCaptureCurrentMembersAndRespectLimit() {
        RealtimeAudienceFacade service = new RealtimeAudienceApplicationService();

        RealtimeAudienceFacade.SnapshotResult snapshot = service.createSnapshot(7L, 100L, "MANUAL", "operator-1");

        assertThat(snapshot)
                .returns(1L, RealtimeAudienceFacade.SnapshotResult::snapshotId)
                .returns(100L, RealtimeAudienceFacade.SnapshotResult::audienceId)
                .returns("MANUAL", RealtimeAudienceFacade.SnapshotResult::reason)
                .returns("operator-1", RealtimeAudienceFacade.SnapshotResult::createdBy)
                .returns(2, RealtimeAudienceFacade.SnapshotResult::memberCount);
        assertThat(service.listSnapshots(7L, 100L, 0)).hasSize(1);

        service.createSnapshot(7L, 100L, "MANUAL", "operator-2");
        assertThat(service.listSnapshots(7L, 100L, 1))
                .singleElement()
                .returns(2L, RealtimeAudienceFacade.SnapshotRow::snapshotId);
    }

    /**
     * 执行 overlapMergeAndExcludeOperateOnAudienceSets 对应的 CDP 业务操作。
     */
    @Test
    void overlapMergeAndExcludeOperateOnAudienceSets() {
        RealtimeAudienceFacade service = new RealtimeAudienceApplicationService();

        RealtimeAudienceFacade.OverlapResult overlap = service.overlap(100L, 200L);
        RealtimeAudienceFacade.SetOperationResult merge = service.merge(100L, 200L);
        RealtimeAudienceFacade.SetOperationResult exclude = service.exclude(100L, 200L);

        assertThat(overlap)
                .returns(100L, RealtimeAudienceFacade.OverlapResult::leftId)
                .returns(200L, RealtimeAudienceFacade.OverlapResult::rightId)
                .returns(1, RealtimeAudienceFacade.OverlapResult::overlapCount);
        assertThat(merge)
                .returns("MERGE", RealtimeAudienceFacade.SetOperationResult::operation)
                .returns(2, RealtimeAudienceFacade.SetOperationResult::memberCount);
        assertThat(merge.memberIds()).containsExactly("user-1", "user-2");
        assertThat(exclude)
                .returns("EXCLUDE", RealtimeAudienceFacade.SetOperationResult::operation)
                .returns(1, RealtimeAudienceFacade.SetOperationResult::memberCount);
        assertThat(exclude.memberIds()).containsExactly("user-2");
    }

    /**
     * 执行 validationUsesCompatibilityMessages 对应的 CDP 业务操作。
     */
    @Test
    void validationUsesCompatibilityMessages() {
        RealtimeAudienceFacade service = new RealtimeAudienceApplicationService();

        assertThatThrownBy(() -> service.processEvent(7L, 999L,
                new RealtimeAudienceFacade.CdpEvent("evt", "user", Instant.now(), Map.of()), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("audience is not found");
        assertThatThrownBy(() -> service.processEvent(7L, 100L,
                new RealtimeAudienceFacade.CdpEvent("evt", " ", Instant.now(), Map.of("tier", "gold")), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId is required");
        assertThatThrownBy(() -> service.createSnapshot(8L, 100L, "MANUAL", "operator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("audience is not found");
    }
}
