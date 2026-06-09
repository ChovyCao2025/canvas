package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.CdpWarehouseStreamPipelineDO;

import java.time.LocalDateTime;

/**
 * CdpWarehouseStreamPipelineMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface CdpWarehouseStreamPipelineMapper extends BaseMapper<CdpWarehouseStreamPipelineDO> {

    @Insert("""
            INSERT INTO cdp_warehouse_stream_pipeline
            (tenant_id, pipeline_key, display_name, source_type, source_ref, source_topic, consumer_group,
             processor_type, sink_type, sink_ref, delivery_semantics, checkpoint_interval_seconds,
             max_lag_ms, max_checkpoint_age_seconds, lifecycle_status, owner_name, config_json)
            VALUES
            (#{row.tenantId}, #{row.pipelineKey}, #{row.displayName}, #{row.sourceType}, #{row.sourceRef},
             #{row.sourceTopic}, #{row.consumerGroup}, #{row.processorType}, #{row.sinkType}, #{row.sinkRef},
             #{row.deliverySemantics}, #{row.checkpointIntervalSeconds}, #{row.maxLagMs},
             #{row.maxCheckpointAgeSeconds}, #{row.lifecycleStatus}, #{row.ownerName}, #{row.configJson})
            ON DUPLICATE KEY UPDATE
                display_name = VALUES(display_name),
                source_type = VALUES(source_type),
                source_ref = VALUES(source_ref),
                source_topic = VALUES(source_topic),
                consumer_group = VALUES(consumer_group),
                processor_type = VALUES(processor_type),
                sink_type = VALUES(sink_type),
                sink_ref = VALUES(sink_ref),
                delivery_semantics = VALUES(delivery_semantics),
                checkpoint_interval_seconds = VALUES(checkpoint_interval_seconds),
                max_lag_ms = VALUES(max_lag_ms),
                max_checkpoint_age_seconds = VALUES(max_checkpoint_age_seconds),
                lifecycle_status = VALUES(lifecycle_status),
                owner_name = VALUES(owner_name),
                config_json = VALUES(config_json),
                updated_at = CURRENT_TIMESTAMP
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回流程执行后的业务结果。
     */
    int upsert(@Param("row") CdpWarehouseStreamPipelineDO row);

    @Update("""
            UPDATE cdp_warehouse_stream_pipeline
            SET last_checkpoint_id = #{checkpoint.lastCheckpointId},
                last_source_offset = #{checkpoint.lastSourceOffset},
                last_committed_offset = #{checkpoint.lastCommittedOffset},
                last_watermark_time = #{checkpoint.lastWatermarkTime},
                last_checkpoint_at = #{checkpoint.lastCheckpointAt},
                last_lag_ms = #{checkpoint.lastLagMs},
                last_runtime_status = #{checkpoint.lastRuntimeStatus},
                last_status_message = #{checkpoint.lastStatusMessage},
                last_reported_by = #{checkpoint.lastReportedBy},
                updated_at = #{now}
            WHERE tenant_id = #{tenantId}
              AND pipeline_key = #{pipelineKey}
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param pipelineKey 业务键，用于在同一租户下定位资源。
     * @param checkpoint checkpoint 参数，用于 updateRuntime 流程中的校验、计算或对象转换。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回流程执行后的业务结果。
     */
    int updateRuntime(@Param("tenantId") Long tenantId,
                      @Param("pipelineKey") String pipelineKey,
                      @Param("checkpoint") CdpWarehouseStreamPipelineDO checkpoint,
                      @Param("now") LocalDateTime now);
}
