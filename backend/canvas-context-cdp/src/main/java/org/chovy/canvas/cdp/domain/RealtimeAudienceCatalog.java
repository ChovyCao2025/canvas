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

public class RealtimeAudienceCatalog {

    private final Map<Long, Map<Long, Audience>> tenants = new LinkedHashMap<>();
    private long nextSnapshotId = 1L;

    public RealtimeAudienceCatalog() {
        seed();
    }

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

    public RealtimeAudienceFacade.SnapshotResult createSnapshot(Long tenantId, Long audienceId, String reason,
                                                                String actor) {
        Audience audience = audience(tenantId, audienceId);
        Snapshot snapshot = new Snapshot(nextSnapshotId++, audienceId, audience.memberIds.size(),
                value(reason, "MANUAL"), value(actor, "system"), timestamp());
        audience.snapshots.add(0, snapshot);
        return new RealtimeAudienceFacade.SnapshotResult(snapshot.snapshotId, snapshot.audienceId, snapshot.reason,
                snapshot.createdBy, snapshot.memberCount, snapshot.createdAt);
    }

    public List<RealtimeAudienceFacade.SnapshotRow> listSnapshots(Long tenantId, Long audienceId, int limit) {
        Audience audience = audience(tenantId, audienceId);
        return audience.snapshots.stream()
                .limit(limit)
                .map(snapshot -> new RealtimeAudienceFacade.SnapshotRow(snapshot.snapshotId, snapshot.audienceId,
                        snapshot.memberCount, snapshot.reason, snapshot.createdBy, snapshot.createdAt))
                .toList();
    }

    public RealtimeAudienceFacade.OverlapResult overlap(Long leftId, Long rightId) {
        Set<String> intersection = members(leftId);
        intersection.retainAll(members(rightId));
        return new RealtimeAudienceFacade.OverlapResult(leftId, rightId, intersection.size(),
                List.copyOf(intersection));
    }

    public RealtimeAudienceFacade.SetOperationResult merge(Long leftId, Long rightId) {
        Set<String> merged = members(leftId);
        merged.addAll(members(rightId));
        return new RealtimeAudienceFacade.SetOperationResult("MERGE", leftId, rightId, merged.size(),
                List.copyOf(merged));
    }

    public RealtimeAudienceFacade.SetOperationResult exclude(Long baseId, Long excludedId) {
        Set<String> result = members(baseId);
        result.removeAll(members(excludedId));
        return new RealtimeAudienceFacade.SetOperationResult("EXCLUDE", baseId, excludedId, result.size(),
                List.copyOf(result));
    }

    private Audience audience(Long tenantId, Long audienceId) {
        Audience audience = tenants.getOrDefault(tenantId, Map.of()).get(audienceId);
        if (audience == null) {
            throw new IllegalArgumentException("audience is not found");
        }
        return audience;
    }

    private Set<String> members(Long audienceId) {
        return tenants.values().stream()
                .map(items -> items.get(audienceId))
                .filter(item -> item != null)
                .findFirst()
                .map(item -> new LinkedHashSet<>(item.memberIds))
                .orElseGet(LinkedHashSet::new);
    }

    private static boolean matches(Audience audience, Map<String, Object> properties) {
        return audience.requiredTier == null || audience.requiredTier.equals(String.valueOf(
                properties == null ? null : properties.get("tier")));
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String timestamp() {
        return LocalDateTime.of(2026, 6, 14, 10, 0, 0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private void seed() {
        Map<Long, Audience> tenantSeven = new LinkedHashMap<>();
        tenantSeven.put(100L, new Audience(100L, "gold", "user-1", "user-2"));
        tenantSeven.put(200L, new Audience(200L, null, "user-1"));
        tenants.put(7L, tenantSeven);
    }

    private static final class Audience {
        private final Long audienceId;
        private final String requiredTier;
        private final Set<String> memberIds = new LinkedHashSet<>();
        private final List<Snapshot> snapshots = new ArrayList<>();

        private Audience(Long audienceId, String requiredTier, String... members) {
            this.audienceId = audienceId;
            this.requiredTier = requiredTier;
            this.memberIds.addAll(List.of(members));
        }
    }

    private record Snapshot(Long snapshotId, Long audienceId, int memberCount, String reason, String createdBy,
                            String createdAt) {
    }
}
