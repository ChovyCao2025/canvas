package org.chovy.canvas.domain.cdp;

import org.chovy.canvas.engine.audience.AudienceBitmapStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.roaringbitmap.RoaringBitmap;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RealtimeAudienceServiceTest {

    private RealtimeAudienceService.EventLogRepository eventLogs;
    private RealtimeAudienceService.AudienceRuleRepository audienceRules;
    private RealtimeAudienceService.SnapshotRepository snapshots;
    private AudienceBitmapStore bitmapStore;
    private RealtimeAudienceService service;

    @BeforeEach
    void setUp() {
        eventLogs = mock(RealtimeAudienceService.EventLogRepository.class);
        audienceRules = mock(RealtimeAudienceService.AudienceRuleRepository.class);
        snapshots = mock(RealtimeAudienceService.SnapshotRepository.class);
        bitmapStore = mock(AudienceBitmapStore.class);
        service = new RealtimeAudienceService(eventLogs, audienceRules, snapshots, bitmapStore, 50_000);
    }

    @Test
    void processEventAddsMatchingUser() throws Exception {
        when(audienceRules.matches(0L, 10L, Map.of("event", "Paid"))).thenReturn(true);
        when(eventLogs.reserve(0L, 10L, "evt-1", "u1", "ADD")).thenReturn(true);
        when(bitmapStore.load(10L)).thenReturn(new RoaringBitmap());

        RealtimeAudienceService.EventResult result = service.processEvent(0L, 10L,
                new RealtimeAudienceService.CdpEvent("evt-1", "u1",
                        Instant.parse("2026-06-03T00:00:00Z"), Map.of("event", "Paid")),
                true);

        assertThat(result.status()).isEqualTo("UPDATED");
        assertThat(result.operation()).isEqualTo("ADD");
        verify(bitmapStore).save(eq(10L), argThat(bitmap -> bitmap.contains(AudienceBitmapStore.toUid("u1"))));
    }

    @Test
    void processEventRemovesUserWhenRemoveOnNoMatchIsTrue() throws Exception {
        RoaringBitmap existing = RoaringBitmap.bitmapOf(AudienceBitmapStore.toUid("u1"));
        when(audienceRules.matches(0L, 10L, Map.of("event", "Refunded"))).thenReturn(false);
        when(eventLogs.reserve(0L, 10L, "evt-2", "u1", "REMOVE")).thenReturn(true);
        when(bitmapStore.load(10L)).thenReturn(existing);

        RealtimeAudienceService.EventResult result = service.processEvent(0L, 10L,
                new RealtimeAudienceService.CdpEvent("evt-2", "u1",
                        Instant.parse("2026-06-03T00:00:01Z"), Map.of("event", "Refunded")),
                true);

        assertThat(result.status()).isEqualTo("UPDATED");
        assertThat(result.operation()).isEqualTo("REMOVE");
        verify(bitmapStore).save(eq(10L), argThat(bitmap -> !bitmap.contains(AudienceBitmapStore.toUid("u1"))));
    }

    @Test
    void processEventSkipsNonMatchingUserWhenRemovalIsDisabled() throws Exception {
        when(audienceRules.matches(0L, 10L, Map.of("event", "Viewed"))).thenReturn(false);

        RealtimeAudienceService.EventResult result = service.processEvent(0L, 10L,
                new RealtimeAudienceService.CdpEvent("evt-3", "u1",
                        Instant.parse("2026-06-03T00:00:02Z"), Map.of("event", "Viewed")),
                false);

        assertThat(result.status()).isEqualTo("SKIPPED");
        assertThat(result.operation()).isEqualTo("NOOP");
        verify(eventLogs, never()).reserve(anyLong(), anyLong(), any(), any(), any());
        verify(bitmapStore, never()).save(anyLong(), any());
    }

    @Test
    void processEventIsIdempotentBySourceEventId() throws Exception {
        when(audienceRules.matches(0L, 10L, Map.of("event", "Paid"))).thenReturn(true);
        when(eventLogs.reserve(0L, 10L, "evt-1", "u1", "ADD")).thenReturn(false);

        RealtimeAudienceService.EventResult result = service.processEvent(0L, 10L,
                new RealtimeAudienceService.CdpEvent("evt-1", "u1",
                        Instant.parse("2026-06-03T00:00:00Z"), Map.of("event", "Paid")),
                true);

        assertThat(result.status()).isEqualTo("DUPLICATED");
        verify(bitmapStore, never()).save(anyLong(), any());
    }

    @Test
    void overlapReturnsCountsAndPercentages() throws Exception {
        when(bitmapStore.load(10L)).thenReturn(RoaringBitmap.bitmapOf(1, 2, 3, 4));
        when(bitmapStore.load(11L)).thenReturn(RoaringBitmap.bitmapOf(3, 4));

        RealtimeAudienceService.OverlapResult result = service.overlap(10L, 11L);

        assertThat(result.leftCount()).isEqualTo(4);
        assertThat(result.rightCount()).isEqualTo(2);
        assertThat(result.intersectionCount()).isEqualTo(2);
        assertThat(result.leftPercentage()).isEqualTo(50.0);
        assertThat(result.rightPercentage()).isEqualTo(100.0);
    }

    @Test
    void mergeAndExclusionBlockAboveSafeSizeLimit() throws Exception {
        service = new RealtimeAudienceService(eventLogs, audienceRules, snapshots, bitmapStore, 2);
        when(bitmapStore.merge(10L, 11L)).thenReturn(RoaringBitmap.bitmapOf(1, 2, 3));
        when(bitmapStore.exclude(10L, 11L)).thenReturn(RoaringBitmap.bitmapOf(1, 2, 3));

        assertThat(service.merge(10L, 11L).status()).isEqualTo("BLOCKED");
        assertThat(service.exclude(10L, 11L).reason()).isEqualTo("SAFE_SIZE_LIMIT_EXCEEDED");
    }

    @Test
    void snapshotStoresAudienceSizeBitmapKeyAndSource() throws Exception {
        when(bitmapStore.load(10L)).thenReturn(RoaringBitmap.bitmapOf(1, 2, 3));

        RealtimeAudienceService.SnapshotResult result = service.createSnapshot(0L, 10L, "MANUAL", "operator-1");

        assertThat(result.estimatedSize()).isEqualTo(3);
        assertThat(result.bitmapKey()).isEqualTo("audience:bitmap:10");
        verify(snapshots).insert(argThat(snapshot ->
                snapshot.estimatedSize() == 3 && snapshot.snapshotSource().equals("MANUAL")));
    }

    @Test
    void listSnapshotsReturnsRecentRows() {
        RealtimeAudienceService.SnapshotRow row = new RealtimeAudienceService.SnapshotRow(
                9L, 0L, 10L, 42L, "audience:bitmap:10", "MANUAL", "operator-1", null);
        when(snapshots.list(0L, 10L, 20)).thenReturn(List.of(row));

        assertThat(service.listSnapshots(0L, 10L, 20)).containsExactly(row);
    }
}
