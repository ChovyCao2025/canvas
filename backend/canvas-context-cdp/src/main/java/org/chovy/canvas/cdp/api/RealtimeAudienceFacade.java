package org.chovy.canvas.cdp.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface RealtimeAudienceFacade {

    EventResult processEvent(Long tenantId, Long audienceId, CdpEvent event, boolean removeOnNoMatch);

    SnapshotResult createSnapshot(Long tenantId, Long audienceId, String reason, String actor);

    List<SnapshotRow> listSnapshots(Long tenantId, Long audienceId, int limit);

    OverlapResult overlap(Long leftId, Long rightId);

    SetOperationResult merge(Long leftId, Long rightId);

    SetOperationResult exclude(Long baseId, Long excludedId);

    record CdpEvent(String sourceEventId, String userId, Instant eventTime, Map<String, Object> properties) {
    }

    record EventResult(Long audienceId, String userId, boolean matched, boolean removed, int memberCount) {
    }

    record SnapshotResult(Long snapshotId, Long audienceId, String reason, String createdBy, int memberCount,
                          String createdAt) {
    }

    record SnapshotRow(Long snapshotId, Long audienceId, int memberCount, String reason, String createdBy,
                       String createdAt) {
    }

    record OverlapResult(Long leftId, Long rightId, int overlapCount, List<String> memberIds) {
    }

    record SetOperationResult(String operation, Long leftId, Long rightId, int memberCount, List<String> memberIds) {
    }
}
