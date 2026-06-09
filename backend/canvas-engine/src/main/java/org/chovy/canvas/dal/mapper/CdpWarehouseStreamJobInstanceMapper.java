package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.CdpWarehouseStreamJobInstanceDO;

import java.util.List;

/**
 * CdpWarehouseStreamJobInstanceMapper 定义 dal.mapper 场景中的扩展契约。
 */
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
    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回流程执行后的业务结果。
     */
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
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param pipelineKey 业务键，用于在同一租户下定位资源。
     * @param jobKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
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
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param pipelineKey 业务键，用于在同一租户下定位资源。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
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
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param pipelineKey 业务键，用于在同一租户下定位资源。
     * @param jobKey 业务键，用于在同一租户下定位资源。
     * @param desiredStatus 业务状态，用于筛选或推进状态流转。
     * @return 返回流程执行后的业务结果。
     */
    int updateDesiredStatus(@Param("tenantId") Long tenantId,
                            @Param("pipelineKey") String pipelineKey,
                            @Param("jobKey") String jobKey,
                            @Param("desiredStatus") String desiredStatus);
}
