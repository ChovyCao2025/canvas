package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.chovy.canvas.dal.dataobject.CdpWarehouseStreamCheckpointDO;

/**
 * CdpWarehouseStreamCheckpointMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface CdpWarehouseStreamCheckpointMapper extends BaseMapper<CdpWarehouseStreamCheckpointDO> {

    @Insert("""
            INSERT INTO cdp_warehouse_stream_checkpoint
            (tenant_id, pipeline_key, checkpoint_id, source_partition, source_offset, committed_offset,
             watermark_time, checkpoint_time, lag_ms, row_count, status, error_message, reported_by,
             source_schema_version, sink_schema_version, schema_status)
            VALUES
            (#{row.tenantId}, #{row.pipelineKey}, #{row.checkpointId}, #{row.sourcePartition},
             #{row.sourceOffset}, #{row.committedOffset}, #{row.watermarkTime}, #{row.checkpointTime},
             #{row.lagMs}, #{row.rowCount}, #{row.status}, #{row.errorMessage}, #{row.reportedBy},
             #{row.sourceSchemaVersion}, #{row.sinkSchemaVersion}, #{row.schemaStatus})
            ON DUPLICATE KEY UPDATE
                source_offset = VALUES(source_offset),
                committed_offset = VALUES(committed_offset),
                watermark_time = VALUES(watermark_time),
                checkpoint_time = VALUES(checkpoint_time),
                lag_ms = VALUES(lag_ms),
                row_count = VALUES(row_count),
                status = VALUES(status),
                error_message = VALUES(error_message),
                reported_by = VALUES(reported_by),
                source_schema_version = VALUES(source_schema_version),
                sink_schema_version = VALUES(sink_schema_version),
                schema_status = VALUES(schema_status)
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回流程执行后的业务结果。
     */
    int upsert(@Param("row") CdpWarehouseStreamCheckpointDO row);
}
