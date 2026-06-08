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
/**
 * BiQuickEngineQueueService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class BiQuickEngineQueueService {

    private static final String DEFAULT_POOL_KEY = "STANDARD";
    private static final int DEFAULT_QUEUE_TIMEOUT_SECONDS = 120;
    private static final int MAX_SNAPSHOT_LIMIT = 200;
    private static final int MAX_BLOCK_REASON_LENGTH = 1000;

    private final BiQuickEngineQueueJobMapper mapper;
    private final Clock clock;

    @Autowired
    /**
     * 初始化 BiQuickEngineQueueService 实例。
     *
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiQuickEngineQueueService(BiQuickEngineQueueJobMapper mapper) {
        this(mapper, Clock.systemUTC());
    }

    /**
     * 初始化 BiQuickEngineQueueService 实例。
     *
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    public BiQuickEngineQueueService(BiQuickEngineQueueJobMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 enqueue 流程生成的业务结果。
     */
    public BiQuickEngineQueueJobView enqueue(Long tenantId, BiQuickEngineQueueAdmissionCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setUpdatedAt(now);
        mapper.insert(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return view(row);
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param poolKey 业务键，用于在同一租户下定位资源。
     * @param workerId 业务对象 ID，用于定位具体记录。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 claimReady 流程生成的业务结果。
     */
    public BiQuickEngineQueueClaimResult claimReady(Long tenantId, String poolKey, String workerId, int limit) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (workerId == null || workerId.isBlank()) {
            throw new IllegalArgumentException("workerId is required");
        }
        int safeLimit = Math.max(1, limit);
        LocalDateTime now = LocalDateTime.now(clock);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        int expired = mapper.expireTimedOut(tenantId, now);
        int claimed = mapper.claimReady(tenantId, normalizePoolKey(poolKey), workerId, now, safeLimit);
        List<BiQuickEngineQueueJobView> jobs = mapper.findClaimed(tenantId, workerId, safeLimit)
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .map(this::view)
                .toList();
        return new BiQuickEngineQueueClaimResult(expired, claimed, jobs);
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param workerId 业务对象 ID，用于定位具体记录。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 claimReadyFair 流程生成的业务结果。
     */
    public BiQuickEngineQueueClaimResult claimReadyFair(String workerId, int limit) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (workerId == null || workerId.isBlank()) {
            throw new IllegalArgumentException("workerId is required");
        }
        int safeLimit = Math.max(1, limit);
        String normalizedWorkerId = workerId.trim();
        LocalDateTime now = LocalDateTime.now(clock);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        int expired = mapper.expireTimedOutAll(now);
        List<FairQueueCursor> cursors = fairQueueCursors(now, safeLimit);
        int claimed = 0;
        boolean madeProgress = true;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param poolKey 业务键，用于在同一租户下定位资源。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 snapshot 流程生成的业务结果。
     */
    public BiQuickEngineQueueSnapshotView snapshot(Long tenantId, String poolKey, String status, int limit) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        String poolFilter = normalizeOptional(poolKey);
        String statusFilter = normalizeOptional(status);
        int safeLimit = Math.min(MAX_SNAPSHOT_LIMIT, Math.max(1, limit));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<BiQuickEngineQueueStatusCount> counts = mapper.countByStatus(tenantId, poolFilter);
        Map<String, Long> countByStatus = new HashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param jobId 业务对象 ID，用于定位具体记录。
     * @param workerId 业务对象 ID，用于定位具体记录。
     * @return 返回 complete claimed 的布尔判断结果。
     */
    public boolean completeClaimed(Long tenantId, Long jobId, String workerId) {
        validateClaimedJobCommand(tenantId, jobId, workerId);
        LocalDateTime now = LocalDateTime.now(clock);
        return mapper.completeClaimed(tenantId, jobId, workerId.trim(), now) > 0;
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param jobId 业务对象 ID，用于定位具体记录。
     * @return 返回 complete queued admission 的布尔判断结果。
     */
    public boolean completeQueuedAdmission(Long tenantId, Long jobId) {
        validateQueuedJobCommand(tenantId, jobId);
        LocalDateTime now = LocalDateTime.now(clock);
        return mapper.completeQueuedAdmission(tenantId, jobId, now) > 0;
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param jobId 业务对象 ID，用于定位具体记录。
     * @param workerId 业务对象 ID，用于定位具体记录。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @return 返回 block claimed 的布尔判断结果。
     */
    public boolean blockClaimed(Long tenantId, Long jobId, String workerId, String reason) {
        validateClaimedJobCommand(tenantId, jobId, workerId);
        LocalDateTime now = LocalDateTime.now(clock);
        return mapper.blockClaimed(tenantId, jobId, workerId.trim(), blockReason(reason), now) > 0;
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param jobId 业务对象 ID，用于定位具体记录。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @return 返回 block queued admission 的布尔判断结果。
     */
    public boolean blockQueuedAdmission(Long tenantId, Long jobId, String reason) {
        validateQueuedJobCommand(tenantId, jobId);
        LocalDateTime now = LocalDateTime.now(clock);
        return mapper.blockQueuedAdmission(tenantId, jobId, blockReason(reason), now) > 0;
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param poolKey 业务键，用于在同一租户下定位资源。
     * @param staleAfterSeconds stale after seconds 参数，用于 recoverStaleClaims 流程中的校验、计算或对象转换。
     * @return 返回 recoverStaleClaims 流程生成的业务结果。
     */
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

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param jobId 业务对象 ID，用于定位具体记录。
     * @param workerId 业务对象 ID，用于定位具体记录。
     */
    private void validateClaimedJobCommand(Long tenantId, Long jobId, String workerId) {
        validateQueuedJobCommand(tenantId, jobId);
        if (workerId == null || workerId.isBlank()) {
            throw new IllegalArgumentException("workerId is required");
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param jobId 业务对象 ID，用于定位具体记录。
     */
    private void validateQueuedJobCommand(Long tenantId, Long jobId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (jobId == null) {
            throw new IllegalArgumentException("jobId is required");
        }
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @return 返回 block reason 生成的文本或业务键。
     */
    private String blockReason(String reason) {
        String normalized = reason == null || reason.isBlank() ? "queue job blocked" : reason.trim();
        if (normalized.length() <= MAX_BLOCK_REASON_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_BLOCK_REASON_LENGTH);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 timeout seconds 计算得到的数量、金额或指标值。
     */
    private int timeoutSeconds(Integer value) {
        return value == null || value <= 0 ? DEFAULT_QUEUE_TIMEOUT_SECONDS : value;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param poolKey 业务键，用于在同一租户下定位资源。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizePoolKey(String poolKey) {
        if (poolKey == null || poolKey.isBlank()) {
            return DEFAULT_POOL_KEY;
        }
        return poolKey.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param poolFilter pool filter 参数，用于 recentRows 流程中的校验、计算或对象转换。
     * @param statusFilter status filter 参数，用于 recentRows 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    private List<BiQuickEngineQueueJobDO> recentRows(Long tenantId, String poolFilter, String statusFilter, int limit) {
        List<BiQuickEngineQueueJobDO> rows = mapper.findRecent(tenantId, poolFilter, statusFilter, limit);
        return rows == null ? List.of() : rows;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 fair queue cursors 汇总后的集合、分页或映射视图。
     */
    private List<FairQueueCursor> fairQueueCursors(LocalDateTime now, int limit) {
        int groupLimit = Math.max(limit, limit * 4);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<BiQuickEngineQueueBacklogView> backlogs = mapper.findReadyBacklogs(now, groupLimit);
        List<FairQueueCursor> cursors = new ArrayList<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (BiQuickEngineQueueBacklogView backlog : backlogs == null ? List.<BiQuickEngineQueueBacklogView>of() : backlogs) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 view 流程生成的业务结果。
     */
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

    /**
     * FairQueueCursor 承载对应领域的业务规则、流程编排和结果转换。
     */
    private static final class FairQueueCursor {
        private final Long tenantId;
        private final String poolKey;
        private int remaining;

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param poolKey 业务键，用于在同一租户下定位资源。
         * @param remaining remaining 参数，用于 FairQueueCursor 流程中的校验、计算或对象转换。
         * @return 返回 FairQueueCursor 流程生成的业务结果。
         */
        private FairQueueCursor(Long tenantId, String poolKey, long remaining) {
            this.tenantId = tenantId;
            this.poolKey = poolKey;
            this.remaining = (int) remaining;
        }
    }
}
