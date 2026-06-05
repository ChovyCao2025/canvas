package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.CdpEventLogDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseRealtimeCheckpointDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseRealtimeRetryDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseRealtimeCheckpointMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseRealtimeRetryMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CdpWarehouseRealtimeCheckpointService {

    public static final String STREAM_CDP_EVENT_ODS = "CDP_EVENT_ODS";

    private static final int MAX_ERROR_LENGTH = 1000;

    private final CdpWarehouseRealtimeCheckpointMapper checkpointMapper;
    private final CdpWarehouseRealtimeRetryMapper retryMapper;

    public CdpWarehouseRealtimeCheckpointService(CdpWarehouseRealtimeCheckpointMapper checkpointMapper,
                                                 CdpWarehouseRealtimeRetryMapper retryMapper) {
        this.checkpointMapper = checkpointMapper;
        this.retryMapper = retryMapper;
    }

    public void recordDelivered(CdpEventLogDO row, String deliverySource) {
        if (row == null || row.getId() == null) {
            return;
        }
        CdpWarehouseRealtimeCheckpointDO checkpoint = baseCheckpoint(row);
        checkpoint.setLastDeliveredAt(LocalDateTime.now());
        checkpoint.setLastDeliverySource(hasText(deliverySource) ? deliverySource.trim() : "UNKNOWN");
        checkpointMapper.upsertDelivered(checkpoint);
    }

    public void recordFailure(CdpEventLogDO row, String errorMessage) {
        if (row == null || row.getId() == null) {
            return;
        }
        CdpWarehouseRealtimeCheckpointDO checkpoint = baseCheckpoint(row);
        checkpoint.setLastFailureAt(LocalDateTime.now());
        checkpoint.setLastFailureMessage(limit(errorMessage));
        checkpointMapper.upsertFailure(checkpoint);
    }

    public RealtimeStatus status(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        List<CdpWarehouseRealtimeCheckpointDO> checkpoints = checkpointMapper.selectList(
                new LambdaQueryWrapper<CdpWarehouseRealtimeCheckpointDO>()
                        .eq(CdpWarehouseRealtimeCheckpointDO::getTenantId, scopedTenantId)
                        .orderByAsc(CdpWarehouseRealtimeCheckpointDO::getStreamKey));
        long liveRetryCount = countRetries(scopedTenantId, List.of("PENDING", "RETRY", "SENDING"));
        long deadRetryCount = countRetries(scopedTenantId, List.of("DEAD"));
        return new RealtimeStatus(
                scopedTenantId,
                checkpoints == null ? List.of() : checkpoints.stream().map(this::toRow).toList(),
                liveRetryCount,
                deadRetryCount);
    }

    private long countRetries(Long tenantId, List<String> statuses) {
        Long count = retryMapper.selectCount(new LambdaQueryWrapper<CdpWarehouseRealtimeRetryDO>()
                .eq(CdpWarehouseRealtimeRetryDO::getTenantId, tenantId)
                .in(CdpWarehouseRealtimeRetryDO::getStatus, statuses));
        return count == null ? 0L : count;
    }

    private CdpWarehouseRealtimeCheckpointDO baseCheckpoint(CdpEventLogDO row) {
        CdpWarehouseRealtimeCheckpointDO checkpoint = new CdpWarehouseRealtimeCheckpointDO();
        checkpoint.setTenantId(normalizeTenant(row.getTenantId()));
        checkpoint.setStreamKey(STREAM_CDP_EVENT_ODS);
        checkpoint.setLastEventLogId(row.getId());
        checkpoint.setLastMessageId(row.getMessageId());
        checkpoint.setLastEventCode(row.getEventCode());
        checkpoint.setLastEventTime(row.getEventTime());
        checkpoint.setLastReceivedAt(row.getReceivedAt());
        return checkpoint;
    }

    private CheckpointRow toRow(CdpWarehouseRealtimeCheckpointDO row) {
        return new CheckpointRow(
                row.getId(),
                row.getStreamKey(),
                row.getLastEventLogId(),
                row.getLastMessageId(),
                row.getLastEventCode(),
                row.getLastEventTime(),
                row.getLastReceivedAt(),
                row.getLastDeliveredAt(),
                row.getLastDeliverySource(),
                nullToZero(row.getDeliveredCount()),
                nullToZero(row.getFailureCount()),
                row.getLastFailureAt(),
                row.getLastFailureMessage(),
                row.getUpdatedAt());
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private String limit(String message) {
        String value = message == null ? "warehouse realtime delivery failed" : message;
        if (value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record RealtimeStatus(
            Long tenantId,
            List<CheckpointRow> checkpoints,
            long liveRetryCount,
            long deadRetryCount) {
    }

    public record CheckpointRow(
            Long id,
            String streamKey,
            Long lastEventLogId,
            String lastMessageId,
            String lastEventCode,
            LocalDateTime lastEventTime,
            LocalDateTime lastReceivedAt,
            LocalDateTime lastDeliveredAt,
            String lastDeliverySource,
            long deliveredCount,
            long failureCount,
            LocalDateTime lastFailureAt,
            String lastFailureMessage,
            LocalDateTime updatedAt) {
    }
}
