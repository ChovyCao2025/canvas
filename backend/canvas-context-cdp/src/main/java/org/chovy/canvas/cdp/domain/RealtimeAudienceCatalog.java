package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.chovy.canvas.cdp.api.RealtimeAudienceFacade;

/**
 * 维护 RealtimeAudience 的内存目录和查询视图。
 */
public class RealtimeAudienceCatalog {

    private final Map<Long, Map<Long, Audience>> tenants = new LinkedHashMap<>();
    /**
     * next Snapshot Id。
     */
    private long nextSnapshotId = 1L;

    /**
     * 创建当前组件实例。
     */
    public RealtimeAudienceCatalog() {
        seed();
    }

    /**
     * 执行 processEvent 对应的 CDP 业务操作。
     */
    public RealtimeAudienceFacade.EventResult processEvent(Long tenantId, Long audienceId,
                                                           RealtimeAudienceFacade.CdpEvent event,
                                                           boolean removeOnNoMatch) {
        Audience audience = audience(tenantId, audienceId);
        String userId = required(event.userId(), "userId is required");
        boolean matched = matches(audience, event.properties());
        boolean removed = false;
        if (matched) {
            audience.memberIds.add(userId);
        } else if (removeOnNoMatch) {
            removed = audience.memberIds.remove(userId);
        }
        return new RealtimeAudienceFacade.EventResult(audienceId, userId, matched, removed,
                audience.memberIds.size());
    }

    /**
     * 创建Snapshot。
     */
    public RealtimeAudienceFacade.SnapshotResult createSnapshot(Long tenantId, Long audienceId, String reason,
                                                                String actor) {
        Audience audience = audience(tenantId, audienceId);
        Snapshot snapshot = new Snapshot(nextSnapshotId++, audienceId, audience.memberIds.size(),
                value(reason, "MANUAL"), value(actor, "system"), timestamp());
        audience.snapshots.add(0, snapshot);
        return new RealtimeAudienceFacade.SnapshotResult(snapshot.snapshotId, snapshot.audienceId, snapshot.reason,
                snapshot.createdBy, snapshot.memberCount, snapshot.createdAt);
    }

    /**
     * 查询Snapshots列表。
     */
    public List<RealtimeAudienceFacade.SnapshotRow> listSnapshots(Long tenantId, Long audienceId, int limit) {
        Audience audience = audience(tenantId, audienceId);
        return audience.snapshots.stream()
                .limit(limit)
                .map(snapshot -> new RealtimeAudienceFacade.SnapshotRow(snapshot.snapshotId, snapshot.audienceId,
                        snapshot.memberCount, snapshot.reason, snapshot.createdBy, snapshot.createdAt))
                .toList();
    }

    /**
     * 执行 overlap 对应的 CDP 业务操作。
     */
    public RealtimeAudienceFacade.OverlapResult overlap(Long leftId, Long rightId) {
        Set<String> intersection = members(leftId);
        intersection.retainAll(members(rightId));
        return new RealtimeAudienceFacade.OverlapResult(leftId, rightId, intersection.size(),
                List.copyOf(intersection));
    }

    /**
     * 执行 merge 对应的 CDP 业务操作。
     */
    public RealtimeAudienceFacade.SetOperationResult merge(Long leftId, Long rightId) {
        Set<String> merged = members(leftId);
        merged.addAll(members(rightId));
        return new RealtimeAudienceFacade.SetOperationResult("MERGE", leftId, rightId, merged.size(),
                List.copyOf(merged));
    }

    /**
     * 执行 exclude 对应的 CDP 业务操作。
     */
    public RealtimeAudienceFacade.SetOperationResult exclude(Long baseId, Long excludedId) {
        Set<String> result = members(baseId);
        result.removeAll(members(excludedId));
        return new RealtimeAudienceFacade.SetOperationResult("EXCLUDE", baseId, excludedId, result.size(),
                List.copyOf(result));
    }

    /**
     * 执行 audience 对应的 CDP 业务操作。
     */
    private Audience audience(Long tenantId, Long audienceId) {
        Audience audience = tenants.getOrDefault(tenantId, Map.of()).get(audienceId);
        if (audience == null) {
            throw new IllegalArgumentException("audience is not found");
        }
        return audience;
    }

    /**
     * 执行 members 对应的 CDP 业务操作。
     */
    private Set<String> members(Long audienceId) {
        return tenants.values().stream()
                .map(items -> items.get(audienceId))
                .filter(item -> item != null)
                .findFirst()
                .map(item -> new LinkedHashSet<>(item.memberIds))
                .orElseGet(LinkedHashSet::new);
    }

    /**
     * 执行 matches 对应的 CDP 业务操作。
     */
    private static boolean matches(Audience audience, Map<String, Object> properties) {
        return audience.requiredTier == null || audience.requiredTier.equals(String.valueOf(
                properties == null ? null : properties.get("tier")));
    }

    /**
     * 读取并校验必填的d。
     */
    private static String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /**
     * 执行 value 对应的 CDP 业务操作。
     */
    private static String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /**
     * 执行 timestamp 对应的 CDP 业务操作。
     */
    private static String timestamp() {
        return LocalDateTime.of(2026, 6, 14, 10, 0, 0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /**
     * 执行 seed 对应的 CDP 业务操作。
     */
    private void seed() {
        Map<Long, Audience> tenantSeven = new LinkedHashMap<>();
        tenantSeven.put(100L, new Audience(100L, "gold", "user-1", "user-2"));
        tenantSeven.put(200L, new Audience(200L, null, "user-1"));
        tenants.put(7L, tenantSeven);
    }

    /**
     * 表示 Audience 的业务数据或处理组件。
     */
    private static final class Audience {
        /**
         * 人群标识。
         */
        private final Long audienceId;

        /**
         * required Tier。
         */
        private final String requiredTier;
        private final Set<String> memberIds = new LinkedHashSet<>();
        private final List<Snapshot> snapshots = new ArrayList<>();

        /**
         * 创建当前组件实例。
         */
        private Audience(Long audienceId, String requiredTier, String... members) {
            this.audienceId = audienceId;
            this.requiredTier = requiredTier;
            this.memberIds.addAll(List.of(members));
        }
    }

    /**
     * 表示 Snapshot 的业务数据或处理组件。
     */
    private static final class Snapshot {

        /**
         * 快照标识。
         */
        private final Long snapshotId;

        /**
         * 人群标识。
         */
        private final Long audienceId;

        /**
         * 成员数量。
         */
        private final int memberCount;

        /**
         * 原因。
         */
        private final String reason;

        /**
         * 创建人。
         */
        private final String createdBy;

        /**
         * 创建时间。
         */
        private final String createdAt;

        /**
         * 使用记录字段创建 Snapshot。
         */
        private Snapshot(
                Long snapshotId,
                Long audienceId,
                int memberCount,
                String reason,
                String createdBy,
                String createdAt) {
            this.snapshotId = snapshotId;
            this.audienceId = audienceId;
            this.memberCount = memberCount;
            this.reason = reason;
            this.createdBy = createdBy;
            this.createdAt = createdAt;
        }

        /**
         * 返回快照标识。
         */
        public Long snapshotId() {
            return snapshotId;
        }

        /**
         * 返回人群标识。
         */
        public Long audienceId() {
            return audienceId;
        }

        /**
         * 返回成员数量。
         */
        public int memberCount() {
            return memberCount;
        }

        /**
         * 返回原因。
         */
        public String reason() {
            return reason;
        }

        /**
         * 返回创建人。
         */
        public String createdBy() {
            return createdBy;
        }

        /**
         * 返回创建时间。
         */
        public String createdAt() {
            return createdAt;
        }

        /**
         * 按所有字段比较 Snapshot。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Snapshot that = (Snapshot) o;
            return java.util.Objects.equals(snapshotId, that.snapshotId)
                    && java.util.Objects.equals(audienceId, that.audienceId)
                    && java.util.Objects.equals(memberCount, that.memberCount)
                    && java.util.Objects.equals(reason, that.reason)
                    && java.util.Objects.equals(createdBy, that.createdBy)
                    && java.util.Objects.equals(createdAt, that.createdAt);
        }

        /**
         * 根据所有字段计算 Snapshot 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(snapshotId, audienceId, memberCount, reason, createdBy, createdAt);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "Snapshot[" + "snapshotId=" + snapshotId + ", audienceId=" + audienceId + ", memberCount=" + memberCount + ", reason=" + reason + ", createdBy=" + createdBy + ", createdAt=" + createdAt + "]";
        }
    }
}
