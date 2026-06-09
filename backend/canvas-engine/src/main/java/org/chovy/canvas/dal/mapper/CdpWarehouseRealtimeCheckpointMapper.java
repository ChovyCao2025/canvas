package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.chovy.canvas.dal.dataobject.CdpWarehouseRealtimeCheckpointDO;

/**
 * CdpWarehouseRealtimeCheckpointMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface CdpWarehouseRealtimeCheckpointMapper extends BaseMapper<CdpWarehouseRealtimeCheckpointDO> {

    @Insert("""
            INSERT INTO cdp_warehouse_realtime_checkpoint
            (tenant_id, stream_key, last_event_log_id, last_message_id, last_event_code, last_event_time,
             last_received_at, last_delivered_at, last_delivery_source, delivered_count, failure_count)
            VALUES
            (#{row.tenantId}, #{row.streamKey}, #{row.lastEventLogId}, #{row.lastMessageId}, #{row.lastEventCode},
             #{row.lastEventTime}, #{row.lastReceivedAt}, #{row.lastDeliveredAt}, #{row.lastDeliverySource}, 1, 0)
            ON DUPLICATE KEY UPDATE
                last_event_log_id = IF(VALUES(last_event_log_id) >= COALESCE(last_event_log_id, 0), VALUES(last_event_log_id), last_event_log_id),
                last_message_id = IF(VALUES(last_event_log_id) >= COALESCE(last_event_log_id, 0), VALUES(last_message_id), last_message_id),
                last_event_code = IF(VALUES(last_event_log_id) >= COALESCE(last_event_log_id, 0), VALUES(last_event_code), last_event_code),
                last_event_time = IF(VALUES(last_event_log_id) >= COALESCE(last_event_log_id, 0), VALUES(last_event_time), last_event_time),
                last_received_at = IF(VALUES(last_event_log_id) >= COALESCE(last_event_log_id, 0), VALUES(last_received_at), last_received_at),
                last_delivered_at = IF(VALUES(last_event_log_id) >= COALESCE(last_event_log_id, 0), VALUES(last_delivered_at), last_delivered_at),
                last_delivery_source = IF(VALUES(last_event_log_id) >= COALESCE(last_event_log_id, 0), VALUES(last_delivery_source), last_delivery_source),
                delivered_count = delivered_count + 1,
                updated_at = CURRENT_TIMESTAMP
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回流程执行后的业务结果。
     */
    int upsertDelivered(@Param("row") CdpWarehouseRealtimeCheckpointDO row);

    @Insert("""
            INSERT INTO cdp_warehouse_realtime_checkpoint
            (tenant_id, stream_key, last_event_log_id, last_message_id, last_event_code, last_event_time,
             last_received_at, delivered_count, failure_count, last_failure_at, last_failure_message)
            VALUES
            (#{row.tenantId}, #{row.streamKey}, #{row.lastEventLogId}, #{row.lastMessageId}, #{row.lastEventCode},
             #{row.lastEventTime}, #{row.lastReceivedAt}, 0, 1, #{row.lastFailureAt}, #{row.lastFailureMessage})
            ON DUPLICATE KEY UPDATE
                failure_count = failure_count + 1,
                last_failure_at = VALUES(last_failure_at),
                last_failure_message = VALUES(last_failure_message),
                updated_at = CURRENT_TIMESTAMP
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回流程执行后的业务结果。
     */
    int upsertFailure(@Param("row") CdpWarehouseRealtimeCheckpointDO row);
}
