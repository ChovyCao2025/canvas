package org.chovy.canvas.domain.bi.dataset;

import org.chovy.canvas.dal.dataobject.BiQuickEngineQueueJobDO;
import org.chovy.canvas.dal.mapper.BiQuickEngineQueueJobMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class BiQuickEngineQueueService {

    private static final String DEFAULT_POOL_KEY = "STANDARD";
    private static final int DEFAULT_QUEUE_TIMEOUT_SECONDS = 120;
    private static final int MAX_SNAPSHOT_LIMIT = 200;
    private static final int MAX_BLOCK_REASON_LENGTH = 1000;

    private final BiQuickEngineQueueJobMapper mapper;
    private final Clock clock;

    @Autowired
    public BiQuickEngineQueueService(BiQuickEngineQueueJobMapper mapper) {
        this(mapper, Clock.systemUTC());
    }

    public BiQuickEngineQueueService(BiQuickEngineQueueJobMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public BiQuickEngineQueueJobView enqueue(Long tenantId, BiQuickEngineQueueAdmissionCommand command) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (command == null) {
            throw new IllegalArgumentException("queue admission command is required");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        BiQuickEngineQueueJobDO row = new BiQuickEngineQueueJobDO();
        row.setTenantId(tenantId);
        row.setPoolKey(normalizePoolKey(command.poolKey()));
        row.setSqlHash(command.sqlHash());
        row.setDatasetKey(command.datasetKey());
        row.setRequestedBy(command.requestedBy());
        row.setStatus("QUEUED");
        row.setAttemptCount(0);
        row.setQueuedAt(now);
        row.setExpiresAt(now.plusSeconds(timeoutSeconds(command.queueTimeoutSeconds())));
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        mapper.insert(row);
        return view(row);
    }

    public BiQuickEngineQueueClaimResult claimReady(Long tenantId, String poolKey, String workerId, int limit) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (workerId == null || workerId.isBlank()) {
            throw new IllegalArgumentException("workerId is required");
        }
        int safeLimit = Math.max(1, limit);
        LocalDateTime now = LocalDateTime.now(clock);
        int expired = mapper.expireTimedOut(tenantId, now);
        int claimed = mapper.claimReady(tenantId, normalizePoolKey(poolKey), workerId, now, safeLimit);
        List<BiQuickEngineQueueJobView> jobs = mapper.findClaimed(tenantId, workerId, safeLimit)
                .stream()
                .map(this::view)
                .toList();
        return new BiQuickEngineQueueClaimResult(expired, claimed, jobs);
    }

    public BiQuickEngineQueueClaimResult claimReadyFair(String workerId, int limit) {
        if (workerId == null || workerId.isBlank()) {
            throw new IllegalArgumentException("workerId is required");
        }
        int safeLimit = Math.max(1, limit);
        String normalizedWorkerId = workerId.trim();
        LocalDateTime now = LocalDateTime.now(clock);
        int expired = mapper.expireTimedOutAll(now);
        List<FairQueueCursor> cursors = fairQueueCursors(now, safeLimit);
        int claimed = 0;
        boolean madeProgress = true;
        while (claimed < safeLimit && madeProgress) {
            madeProgress = false;
            for (FairQueueCursor cursor : cursors) {
                if (claimed >= safeLimit) {
                    break;
                }
                if (cursor.remaining <= 0) {
                    continue;
                }
                int claimedFromPool = Math.max(0, mapper.claimReady(
                        cursor.tenantId,
                        cursor.poolKey,
                        normalizedWorkerId,
                        now,
                        1));
                if (claimedFromPool <= 0) {
                    cursor.remaining = 0;
                    continue;
                }
                int accepted = Math.min(claimedFromPool, safeLimit - claimed);
                claimed += accepted;
                cursor.remaining = Math.max(0, cursor.remaining - accepted);
                madeProgress = true;
            }
        }
        List<BiQuickEngineQueueJobDO> claimedRows = mapper.findClaimedByWorker(normalizedWorkerId, now, safeLimit);
        List<BiQuickEngineQueueJobView> jobs = (claimedRows == null ? List.<BiQuickEngineQueueJobDO>of() : claimedRows)
                .stream()
                .map(this::view)
                .toList();
        return new BiQuickEngineQueueClaimResult(expired, claimed, jobs);
    }

    public BiQuickEngineQueueSnapshotView snapshot(Long tenantId, String poolKey, String status, int limit) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        String poolFilter = normalizeOptional(poolKey);
        String statusFilter = normalizeOptional(status);
        int safeLimit = Math.min(MAX_SNAPSHOT_LIMIT, Math.max(1, limit));
        List<BiQuickEngineQueueStatusCount> counts = mapper.countByStatus(tenantId, poolFilter);
        Map<String, Long> countByStatus = new HashMap<>();
        for (BiQuickEngineQueueStatusCount count : counts == null ? List.<BiQuickEngineQueueStatusCount>of() : counts) {
            String key = normalizeOptional(count.getStatus());
            if (key != null) {
                countByStatus.merge(key, count.getCount() == null ? 0L : count.getCount(), Long::sum);
            }
        }
        List<BiQuickEngineQueueJobView> jobs = recentRows(tenantId, poolFilter, statusFilter, safeLimit)
                .stream()
                .map(this::view)
                .toList();
        long queued = countByStatus.getOrDefault("QUEUED", 0L);
        long claimed = countByStatus.getOrDefault("CLAIMED", 0L);
        long completed = countByStatus.getOrDefault("COMPLETED", 0L);
        long blocked = countByStatus.getOrDefault("BLOCKED", 0L);
        long total = countByStatus.values().stream().mapToLong(Long::longValue).sum();
        return new BiQuickEngineQueueSnapshotView(
                tenantId,
                poolFilter,
                queued,
                claimed,
                completed,
                blocked,
                total,
                jobs);
    }

    public boolean completeClaimed(Long tenantId, Long jobId, String workerId) {
        validateClaimedJobCommand(tenantId, jobId, workerId);
        LocalDateTime now = LocalDateTime.now(clock);
        return mapper.completeClaimed(tenantId, jobId, workerId.trim(), now) > 0;
    }

    public boolean completeQueuedAdmission(Long tenantId, Long jobId) {
        validateQueuedJobCommand(tenantId, jobId);
        LocalDateTime now = LocalDateTime.now(clock);
        return mapper.completeQueuedAdmission(tenantId, jobId, now) > 0;
    }

    public boolean blockClaimed(Long tenantId, Long jobId, String workerId, String reason) {
        validateClaimedJobCommand(tenantId, jobId, workerId);
        LocalDateTime now = LocalDateTime.now(clock);
        return mapper.blockClaimed(tenantId, jobId, workerId.trim(), blockReason(reason), now) > 0;
    }

    public boolean blockQueuedAdmission(Long tenantId, Long jobId, String reason) {
        validateQueuedJobCommand(tenantId, jobId);
        LocalDateTime now = LocalDateTime.now(clock);
        return mapper.blockQueuedAdmission(tenantId, jobId, blockReason(reason), now) > 0;
    }

    public BiQuickEngineQueueRecoveryResult recoverStaleClaims(Long tenantId,
                                                               String poolKey,
                                                               int staleAfterSeconds) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        String normalizedPoolKey = normalizePoolKey(poolKey);
        LocalDateTime now = LocalDateTime.now(clock);
        int expired = mapper.expireStaleClaimed(tenantId, normalizedPoolKey, now);
        int recovered = mapper.recoverStaleClaims(
                tenantId,
                normalizedPoolKey,
                now.minusSeconds(timeoutSeconds(staleAfterSeconds)),
                now);
        return new BiQuickEngineQueueRecoveryResult(expired, recovered);
    }

    private void validateClaimedJobCommand(Long tenantId, Long jobId, String workerId) {
        validateQueuedJobCommand(tenantId, jobId);
        if (workerId == null || workerId.isBlank()) {
            throw new IllegalArgumentException("workerId is required");
        }
    }

    private void validateQueuedJobCommand(Long tenantId, Long jobId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (jobId == null) {
            throw new IllegalArgumentException("jobId is required");
        }
    }

    private String blockReason(String reason) {
        String normalized = reason == null || reason.isBlank() ? "queue job blocked" : reason.trim();
        if (normalized.length() <= MAX_BLOCK_REASON_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_BLOCK_REASON_LENGTH);
    }

    private int timeoutSeconds(Integer value) {
        return value == null || value <= 0 ? DEFAULT_QUEUE_TIMEOUT_SECONDS : value;
    }

    private String normalizePoolKey(String poolKey) {
        if (poolKey == null || poolKey.isBlank()) {
            return DEFAULT_POOL_KEY;
        }
        return poolKey.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private List<BiQuickEngineQueueJobDO> recentRows(Long tenantId, String poolFilter, String statusFilter, int limit) {
        List<BiQuickEngineQueueJobDO> rows = mapper.findRecent(tenantId, poolFilter, statusFilter, limit);
        return rows == null ? List.of() : rows;
    }

    private List<FairQueueCursor> fairQueueCursors(LocalDateTime now, int limit) {
        int groupLimit = Math.max(limit, limit * 4);
        List<BiQuickEngineQueueBacklogView> backlogs = mapper.findReadyBacklogs(now, groupLimit);
        List<FairQueueCursor> cursors = new ArrayList<>();
        for (BiQuickEngineQueueBacklogView backlog : backlogs == null ? List.<BiQuickEngineQueueBacklogView>of() : backlogs) {
            if (backlog == null || backlog.getTenantId() == null) {
                continue;
            }
            long readyCount = backlog.getReadyCount() == null ? 0L : backlog.getReadyCount();
            if (readyCount <= 0) {
                continue;
            }
            cursors.add(new FairQueueCursor(
                    backlog.getTenantId(),
                    normalizePoolKey(backlog.getPoolKey()),
                    Math.min(Integer.MAX_VALUE, readyCount)));
        }
        return cursors;
    }

    private BiQuickEngineQueueJobView view(BiQuickEngineQueueJobDO row) {
        return new BiQuickEngineQueueJobView(
                row.getId(),
                row.getTenantId(),
                row.getPoolKey(),
                row.getSqlHash(),
                row.getDatasetKey(),
                row.getRequestedBy(),
                row.getStatus(),
                row.getAttemptCount() == null ? 0 : row.getAttemptCount(),
                row.getQueuedAt(),
                row.getExpiresAt(),
                row.getClaimedBy(),
                row.getClaimedAt(),
                row.getCompletedAt(),
                row.getBlockedReason(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private static final class FairQueueCursor {
        private final Long tenantId;
        private final String poolKey;
        private int remaining;

        private FairQueueCursor(Long tenantId, String poolKey, long remaining) {
            this.tenantId = tenantId;
            this.poolKey = poolKey;
            this.remaining = (int) remaining;
        }
    }
}
