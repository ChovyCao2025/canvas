package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.CdpWarehouseStreamJobInstanceDO;

import java.util.List;

@Mapper
public interface CdpWarehouseStreamJobInstanceMapper extends BaseMapper<CdpWarehouseStreamJobInstanceDO> {

    @Insert("""
            INSERT INTO cdp_warehouse_stream_job_instance
            (tenant_id, pipeline_key, job_key, engine_type, engine_job_id, deployment_ref,
             runtime_status, desired_status, last_heartbeat_at, heartbeat_payload_json,
             last_error_message, owner_name)
            VALUES
            (#{row.tenantId}, #{row.pipelineKey}, #{row.jobKey}, #{row.engineType}, #{row.engineJobId},
             #{row.deploymentRef}, #{row.runtimeStatus}, #{row.desiredStatus}, #{row.lastHeartbeatAt},
             #{row.heartbeatPayloadJson}, #{row.lastErrorMessage}, #{row.ownerName})
            ON DUPLICATE KEY UPDATE
                engine_type = VALUES(engine_type),
                engine_job_id = VALUES(engine_job_id),
                deployment_ref = VALUES(deployment_ref),
                runtime_status = VALUES(runtime_status),
                desired_status = VALUES(desired_status),
                last_heartbeat_at = VALUES(last_heartbeat_at),
                heartbeat_payload_json = VALUES(heartbeat_payload_json),
                last_error_message = VALUES(last_error_message),
                owner_name = VALUES(owner_name),
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsertHeartbeat(@Param("row") CdpWarehouseStreamJobInstanceDO row);

    @Select("""
            SELECT id, tenant_id, pipeline_key, job_key, engine_type, engine_job_id, deployment_ref,
                   runtime_status, desired_status, last_heartbeat_at, heartbeat_payload_json,
                   last_error_message, owner_name, created_at, updated_at
            FROM cdp_warehouse_stream_job_instance
            WHERE tenant_id = #{tenantId}
              AND pipeline_key = #{pipelineKey}
              AND job_key = #{jobKey}
            LIMIT 1
            """)
    CdpWarehouseStreamJobInstanceDO findByKey(@Param("tenantId") Long tenantId,
                                              @Param("pipelineKey") String pipelineKey,
                                              @Param("jobKey") String jobKey);

    @Select("""
            SELECT id, tenant_id, pipeline_key, job_key, engine_type, engine_job_id, deployment_ref,
                   runtime_status, desired_status, last_heartbeat_at, heartbeat_payload_json,
                   last_error_message, owner_name, created_at, updated_at
            FROM cdp_warehouse_stream_job_instance
            WHERE tenant_id = #{tenantId}
              AND (#{pipelineKey} IS NULL OR pipeline_key = #{pipelineKey})
            ORDER BY last_heartbeat_at DESC, id DESC
            LIMIT #{limit}
            """)
    List<CdpWarehouseStreamJobInstanceDO> listInstances(@Param("tenantId") Long tenantId,
                                                        @Param("pipelineKey") String pipelineKey,
                                                        @Param("limit") int limit);

    @Update("""
            UPDATE cdp_warehouse_stream_job_instance
            SET desired_status = #{desiredStatus},
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND pipeline_key = #{pipelineKey}
              AND job_key = #{jobKey}
            """)
    int updateDesiredStatus(@Param("tenantId") Long tenantId,
                            @Param("pipelineKey") String pipelineKey,
                            @Param("jobKey") String jobKey,
                            @Param("desiredStatus") String desiredStatus);
}
