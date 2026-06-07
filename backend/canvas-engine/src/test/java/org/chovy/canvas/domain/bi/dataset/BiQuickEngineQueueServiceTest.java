package org.chovy.canvas.domain.bi.dataset;

import org.chovy.canvas.dal.dataobject.BiQuickEngineQueueJobDO;
import org.chovy.canvas.dal.mapper.BiQuickEngineQueueJobMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiQuickEngineQueueServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-06T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void enqueuesDurableQuickEngineQueueJobWithTimeoutMetadata() {
        BiQuickEngineQueueJobMapper mapper = mock(BiQuickEngineQueueJobMapper.class);
        when(mapper.insert(any(BiQuickEngineQueueJobDO.class))).thenAnswer(invocation -> {
            BiQuickEngineQueueJobDO row = invocation.getArgument(0);
            row.setId(41L);
            return 1;
        });
        BiQuickEngineQueueService service = new BiQuickEngineQueueService(mapper, CLOCK);

        BiQuickEngineQueueJobView view = service.enqueue(7L, new BiQuickEngineQueueAdmissionCommand(
                "gold",
                "hash-1",
                "canvas_daily_stats",
                "alice",
                30));

        ArgumentCaptor<BiQuickEngineQueueJobDO> saved = ArgumentCaptor.forClass(BiQuickEngineQueueJobDO.class);
        verify(mapper).insert(saved.capture());
        assertThat(saved.getValue()).satisfies(row -> {
            assertThat(row.getTenantId()).isEqualTo(7L);
            assertThat(row.getPoolKey()).isEqualTo("GOLD");
            assertThat(row.getSqlHash()).isEqualTo("hash-1");
            assertThat(row.getDatasetKey()).isEqualTo("canvas_daily_stats");
            assertThat(row.getRequestedBy()).isEqualTo("alice");
            assertThat(row.getStatus()).isEqualTo("QUEUED");
            assertThat(row.getAttemptCount()).isZero();
            assertThat(row.getQueuedAt()).isEqualTo(LocalDateTime.of(2026, 6, 6, 12, 0));
            assertThat(row.getExpiresAt()).isEqualTo(LocalDateTime.of(2026, 6, 6, 12, 0, 30));
            assertThat(row.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 6, 6, 12, 0));
            assertThat(row.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 6, 6, 12, 0));
        });
        assertThat(view.id()).isEqualTo(41L);
        assertThat(view.status()).isEqualTo("QUEUED");
        assertThat(view.expiresAt()).isEqualTo(LocalDateTime.of(2026, 6, 6, 12, 0, 30));
    }

    @Test
    void claimsReadyQueueJobsAfterExpiringTimedOutRows() {
        BiQuickEngineQueueJobMapper mapper = mock(BiQuickEngineQueueJobMapper.class);
        LocalDateTime now = LocalDateTime.of(2026, 6, 6, 12, 0);
        when(mapper.expireTimedOut(7L, now)).thenReturn(2);
        when(mapper.claimReady(7L, "GOLD", "worker-1", now, 3)).thenReturn(1);
        when(mapper.findClaimed(7L, "worker-1", 3)).thenReturn(List.of(job(
                51L,
                "GOLD",
                "hash-2",
                "sales",
                "bob",
                "CLAIMED",
                now.minusSeconds(10),
                now.plusSeconds(50),
                "worker-1",
                now)));
        BiQuickEngineQueueService service = new BiQuickEngineQueueService(mapper, CLOCK);

        BiQuickEngineQueueClaimResult result = service.claimReady(7L, "gold", "worker-1", 3);

        verify(mapper).expireTimedOut(7L, now);
        verify(mapper).claimReady(7L, "GOLD", "worker-1", now, 3);
        verify(mapper).findClaimed(7L, "worker-1", 3);
        assertThat(result.expired()).isEqualTo(2);
        assertThat(result.claimed()).isEqualTo(1);
        assertThat(result.jobs()).singleElement().satisfies(job -> {
            assertThat(job.id()).isEqualTo(51L);
            assertThat(job.poolKey()).isEqualTo("GOLD");
            assertThat(job.sqlHash()).isEqualTo("hash-2");
            assertThat(job.status()).isEqualTo("CLAIMED");
            assertThat(job.claimedBy()).isEqualTo("worker-1");
        });
    }

    @Test
    void claimsReadyQueueJobsFairlyAcrossTenantPools() {
        BiQuickEngineQueueJobMapper mapper = mock(BiQuickEngineQueueJobMapper.class);
        LocalDateTime now = LocalDateTime.of(2026, 6, 6, 12, 0);
        when(mapper.expireTimedOutAll(now)).thenReturn(1);
        when(mapper.findReadyBacklogs(now, 12)).thenReturn(List.of(
                new BiQuickEngineQueueBacklogView(7L, "GOLD", 3L, now.minusSeconds(30)),
                new BiQuickEngineQueueBacklogView(8L, "SILVER", 2L, now.minusSeconds(20))));
        when(mapper.claimReady(7L, "GOLD", "worker-1", now, 1)).thenReturn(1);
        when(mapper.claimReady(8L, "SILVER", "worker-1", now, 1)).thenReturn(1);
        when(mapper.findClaimedByWorker("worker-1", now, 3)).thenReturn(List.of(
                job(7L, 71L, "GOLD", "hash-gold-1", "sales", "alice", "CLAIMED",
                        now.minusSeconds(30), now.plusSeconds(30), "worker-1", now),
                job(8L, 81L, "SILVER", "hash-silver-1", "orders", "bob", "CLAIMED",
                        now.minusSeconds(20), now.plusSeconds(40), "worker-1", now),
                job(7L, 72L, "GOLD", "hash-gold-2", "sales", "alice", "CLAIMED",
                        now.minusSeconds(10), now.plusSeconds(50), "worker-1", now)));
        BiQuickEngineQueueService service = new BiQuickEngineQueueService(mapper, CLOCK);

        BiQuickEngineQueueClaimResult result = service.claimReadyFair("worker-1", 3);

        assertThat(result.expired()).isEqualTo(1);
        assertThat(result.claimed()).isEqualTo(3);
        assertThat(result.jobs()).extracting(BiQuickEngineQueueJobView::tenantId)
                .containsExactly(7L, 8L, 7L);
        assertThat(result.jobs()).extracting(BiQuickEngineQueueJobView::poolKey)
                .containsExactly("GOLD", "SILVER", "GOLD");
        org.mockito.InOrder orderedClaims = inOrder(mapper);
        orderedClaims.verify(mapper).claimReady(7L, "GOLD", "worker-1", now, 1);
        orderedClaims.verify(mapper).claimReady(8L, "SILVER", "worker-1", now, 1);
        orderedClaims.verify(mapper).claimReady(7L, "GOLD", "worker-1", now, 1);
        verify(mapper).findClaimedByWorker("worker-1", now, 3);
    }

    @Test
    void returnsTenantScopedQueueSnapshotWithStatusCountsAndRecentJobs() {
        BiQuickEngineQueueJobMapper mapper = mock(BiQuickEngineQueueJobMapper.class);
        LocalDateTime now = LocalDateTime.of(2026, 6, 6, 12, 0);
        when(mapper.countByStatus(7L, "GOLD")).thenReturn(List.of(
                new BiQuickEngineQueueStatusCount("QUEUED", 4L),
                new BiQuickEngineQueueStatusCount("CLAIMED", 2L),
                new BiQuickEngineQueueStatusCount("COMPLETED", 9L),
                new BiQuickEngineQueueStatusCount("BLOCKED", 1L)));
        when(mapper.findRecent(7L, "GOLD", "QUEUED", 25)).thenReturn(List.of(job(
                61L,
                "GOLD",
                "hash-3",
                "canvas_daily_stats",
                "alice",
                "QUEUED",
                now.minusSeconds(20),
                now.plusSeconds(100),
                null,
                null)));
        BiQuickEngineQueueService service = new BiQuickEngineQueueService(mapper, CLOCK);

        BiQuickEngineQueueSnapshotView snapshot = service.snapshot(7L, " gold ", " queued ", 25);

        verify(mapper).countByStatus(7L, "GOLD");
        verify(mapper).findRecent(7L, "GOLD", "QUEUED", 25);
        assertThat(snapshot.tenantId()).isEqualTo(7L);
        assertThat(snapshot.poolKey()).isEqualTo("GOLD");
        assertThat(snapshot.queued()).isEqualTo(4L);
        assertThat(snapshot.claimed()).isEqualTo(2L);
        assertThat(snapshot.completed()).isEqualTo(9L);
        assertThat(snapshot.blocked()).isEqualTo(1L);
        assertThat(snapshot.total()).isEqualTo(16L);
        assertThat(snapshot.jobs()).singleElement().satisfies(job -> {
            assertThat(job.id()).isEqualTo(61L);
            assertThat(job.status()).isEqualTo("QUEUED");
            assertThat(job.sqlHash()).isEqualTo("hash-3");
        });
    }

    @Test
    void clampsQueueSnapshotLimitAndSupportsAllPoolStatusSummary() {
        BiQuickEngineQueueJobMapper mapper = mock(BiQuickEngineQueueJobMapper.class);
        LocalDateTime now = LocalDateTime.of(2026, 6, 6, 12, 0);
        when(mapper.countByStatus(7L, null)).thenReturn(List.of(
                new BiQuickEngineQueueStatusCount("QUEUED", 3L),
                new BiQuickEngineQueueStatusCount("BLOCKED", 2L)));
        when(mapper.findRecent(7L, null, null, 200)).thenReturn(List.of(job(
                62L,
                "STANDARD",
                "hash-4",
                "sales",
                "bob",
                "BLOCKED",
                now.minusSeconds(40),
                now.minusSeconds(1),
                null,
                null)));
        BiQuickEngineQueueService service = new BiQuickEngineQueueService(mapper, CLOCK);

        BiQuickEngineQueueSnapshotView snapshot = service.snapshot(7L, " ", " ", 500);

        verify(mapper).countByStatus(7L, null);
        verify(mapper).findRecent(7L, null, null, 200);
        assertThat(snapshot.poolKey()).isNull();
        assertThat(snapshot.queued()).isEqualTo(3L);
        assertThat(snapshot.claimed()).isZero();
        assertThat(snapshot.completed()).isZero();
        assertThat(snapshot.blocked()).isEqualTo(2L);
        assertThat(snapshot.total()).isEqualTo(5L);
        assertThat(snapshot.jobs()).singleElement()
                .satisfies(job -> assertThat(job.poolKey()).isEqualTo("STANDARD"));
    }

    @Test
    void completesClaimedQueueJobForOwningWorker() {
        BiQuickEngineQueueJobMapper mapper = mock(BiQuickEngineQueueJobMapper.class);
        LocalDateTime now = LocalDateTime.of(2026, 6, 6, 12, 0);
        when(mapper.completeClaimed(7L, 51L, "worker-1", now)).thenReturn(1);
        BiQuickEngineQueueService service = new BiQuickEngineQueueService(mapper, CLOCK);

        boolean completed = service.completeClaimed(7L, 51L, "worker-1");

        assertThat(completed).isTrue();
        verify(mapper).completeClaimed(7L, 51L, "worker-1", now);
    }

    @Test
    void completesQueuedAdmissionJobAfterSynchronousExecution() {
        BiQuickEngineQueueJobMapper mapper = mock(BiQuickEngineQueueJobMapper.class);
        LocalDateTime now = LocalDateTime.of(2026, 6, 6, 12, 0);
        when(mapper.completeQueuedAdmission(7L, 41L, now)).thenReturn(1);
        BiQuickEngineQueueService service = new BiQuickEngineQueueService(mapper, CLOCK);

        boolean completed = service.completeQueuedAdmission(7L, 41L);

        assertThat(completed).isTrue();
        verify(mapper).completeQueuedAdmission(7L, 41L, now);
    }

    @Test
    void blocksClaimedQueueJobForOwningWorkerWithReason() {
        BiQuickEngineQueueJobMapper mapper = mock(BiQuickEngineQueueJobMapper.class);
        LocalDateTime now = LocalDateTime.of(2026, 6, 6, 12, 0);
        when(mapper.blockClaimed(7L, 51L, "worker-1", "datasource unavailable", now)).thenReturn(1);
        BiQuickEngineQueueService service = new BiQuickEngineQueueService(mapper, CLOCK);

        boolean blocked = service.blockClaimed(7L, 51L, "worker-1", " datasource unavailable ");

        assertThat(blocked).isTrue();
        verify(mapper).blockClaimed(7L, 51L, "worker-1", "datasource unavailable", now);
    }

    @Test
    void blocksQueuedAdmissionJobAfterSynchronousExecutionFailure() {
        BiQuickEngineQueueJobMapper mapper = mock(BiQuickEngineQueueJobMapper.class);
        LocalDateTime now = LocalDateTime.of(2026, 6, 6, 12, 0);
        when(mapper.blockQueuedAdmission(7L, 41L, "warehouse unavailable", now)).thenReturn(1);
        BiQuickEngineQueueService service = new BiQuickEngineQueueService(mapper, CLOCK);

        boolean blocked = service.blockQueuedAdmission(7L, 41L, " warehouse unavailable ");

        assertThat(blocked).isTrue();
        verify(mapper).blockQueuedAdmission(7L, 41L, "warehouse unavailable", now);
    }

    @Test
    void recoversStaleClaimedQueueJobsAndExpiresClaimedJobsPastDeadline() {
        BiQuickEngineQueueJobMapper mapper = mock(BiQuickEngineQueueJobMapper.class);
        LocalDateTime now = LocalDateTime.of(2026, 6, 6, 12, 0);
        when(mapper.expireStaleClaimed(7L, "GOLD", now)).thenReturn(1);
        when(mapper.recoverStaleClaims(7L, "GOLD", now.minusSeconds(90), now)).thenReturn(2);
        BiQuickEngineQueueService service = new BiQuickEngineQueueService(mapper, CLOCK);

        BiQuickEngineQueueRecoveryResult result = service.recoverStaleClaims(7L, "gold", 90);

        assertThat(result.expired()).isEqualTo(1);
        assertThat(result.recovered()).isEqualTo(2);
        verify(mapper).expireStaleClaimed(7L, "GOLD", now);
        verify(mapper).recoverStaleClaims(7L, "GOLD", now.minusSeconds(90), now);
    }

    private BiQuickEngineQueueJobDO job(Long id,
                                        String poolKey,
                                        String sqlHash,
                                        String datasetKey,
                                        String requestedBy,
                                        String status,
                                        LocalDateTime queuedAt,
                                        LocalDateTime expiresAt,
                                        String claimedBy,
                                        LocalDateTime claimedAt) {
        return job(7L, id, poolKey, sqlHash, datasetKey, requestedBy, status, queuedAt, expiresAt, claimedBy, claimedAt);
    }

    private BiQuickEngineQueueJobDO job(Long tenantId,
                                        Long id,
                                        String poolKey,
                                        String sqlHash,
                                        String datasetKey,
                                        String requestedBy,
                                        String status,
                                        LocalDateTime queuedAt,
                                        LocalDateTime expiresAt,
                                        String claimedBy,
                                        LocalDateTime claimedAt) {
        BiQuickEngineQueueJobDO row = new BiQuickEngineQueueJobDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setPoolKey(poolKey);
        row.setSqlHash(sqlHash);
        row.setDatasetKey(datasetKey);
        row.setRequestedBy(requestedBy);
        row.setStatus(status);
        row.setAttemptCount(1);
        row.setQueuedAt(queuedAt);
        row.setExpiresAt(expiresAt);
        row.setClaimedBy(claimedBy);
        row.setClaimedAt(claimedAt);
        row.setCreatedAt(queuedAt);
        row.setUpdatedAt(claimedAt);
        return row;
    }
}
