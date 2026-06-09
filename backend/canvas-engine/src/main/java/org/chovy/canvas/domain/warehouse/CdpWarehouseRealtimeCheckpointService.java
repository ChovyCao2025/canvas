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
/**
 * CdpWarehouseRealtimeCheckpointService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehouseRealtimeCheckpointService {

    public static final String STREAM_CDP_EVENT_ODS = "CDP_EVENT_ODS";

    private static final int MAX_ERROR_LENGTH = 1000;

    private final CdpWarehouseRealtimeCheckpointMapper checkpointMapper;
    private final CdpWarehouseRealtimeRetryMapper retryMapper;

    /**
     * 初始化 CdpWarehouseRealtimeCheckpointService 实例。
     *
     * @param checkpointMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param retryMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseRealtimeCheckpointService(CdpWarehouseRealtimeCheckpointMapper checkpointMapper,
                                                 CdpWarehouseRealtimeRetryMapper retryMapper) {
        this.checkpointMapper = checkpointMapper;
        this.retryMapper = retryMapper;
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param deliverySource delivery source 参数，用于 recordDelivered 流程中的校验、计算或对象转换。
     */
    public void recordDelivered(CdpEventLogDO row, String deliverySource) {
        if (row == null || row.getId() == null) {
            return;
        }
        CdpWarehouseRealtimeCheckpointDO checkpoint = baseCheckpoint(row);
        checkpoint.setLastDeliveredAt(LocalDateTime.now());
        checkpoint.setLastDeliverySource(hasText(deliverySource) ? deliverySource.trim() : "UNKNOWN");
        checkpointMapper.upsertDelivered(checkpoint);
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param errorMessage error message 参数，用于 recordFailure 流程中的校验、计算或对象转换。
     */
    public void recordFailure(CdpEventLogDO row, String errorMessage) {
        if (row == null || row.getId() == null) {
            return;
        }
        CdpWarehouseRealtimeCheckpointDO checkpoint = baseCheckpoint(row);
        checkpoint.setLastFailureAt(LocalDateTime.now());
        checkpoint.setLastFailureMessage(limit(errorMessage));
        checkpointMapper.upsertFailure(checkpoint);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 status 流程生成的业务结果。
     */
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

    /**
     * 统计符合条件的数据规模或状态数量。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param statuses 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回统计数量。
     */
    private long countRetries(Long tenantId, List<String> statuses) {
        Long count = retryMapper.selectCount(new LambdaQueryWrapper<CdpWarehouseRealtimeRetryDO>()
                .eq(CdpWarehouseRealtimeRetryDO::getTenantId, tenantId)
                .in(CdpWarehouseRealtimeRetryDO::getStatus, statuses));
        return count == null ? 0L : count;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 base checkpoint 计算得到的数量、金额或指标值。
     */
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

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 null to zero 计算得到的数量、金额或指标值。
     */
    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String limit(String message) {
        String value = message == null ? "warehouse realtime delivery failed" : message;
        if (value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * RealtimeStatus 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record RealtimeStatus(
            Long tenantId,
            List<CheckpointRow> checkpoints,
            long liveRetryCount,
            long deadRetryCount) {
    }

    /**
     * CheckpointRow 承载对应领域的业务规则、流程编排和结果转换。
     */
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
