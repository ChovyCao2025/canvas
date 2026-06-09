package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.CdpWarehouseExternalRealtimeJobProbeTargetDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CdpWarehouseExternalRealtimeJobProbeTargetMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface CdpWarehouseExternalRealtimeJobProbeTargetMapper
        extends BaseMapper<CdpWarehouseExternalRealtimeJobProbeTargetDO> {

    @Insert("""
            INSERT INTO cdp_warehouse_external_realtime_job_probe_target
            (tenant_id, pipeline_key, job_key, engine_type, endpoint_url, auth_ref,
             external_job_id, connector_name, deployment_ref, enabled, owner_name,
             max_staleness_seconds, config_json)
            VALUES
            (#{row.tenantId}, #{row.pipelineKey}, #{row.jobKey}, #{row.engineType}, #{row.endpointUrl},
             #{row.authRef}, #{row.externalJobId}, #{row.connectorName}, #{row.deploymentRef},
             #{row.enabled}, #{row.ownerName}, #{row.maxStalenessSeconds}, #{row.configJson})
            ON DUPLICATE KEY UPDATE
                engine_type = VALUES(engine_type),
                endpoint_url = VALUES(endpoint_url),
                auth_ref = VALUES(auth_ref),
                external_job_id = VALUES(external_job_id),
                connector_name = VALUES(connector_name),
                deployment_ref = VALUES(deployment_ref),
                enabled = VALUES(enabled),
                owner_name = VALUES(owner_name),
                max_staleness_seconds = VALUES(max_staleness_seconds),
                config_json = VALUES(config_json),
                updated_at = CURRENT_TIMESTAMP
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回流程执行后的业务结果。
     */
    int upsert(@Param("row") CdpWarehouseExternalRealtimeJobProbeTargetDO row);

    @Select("""
            SELECT id, tenant_id, pipeline_key, job_key, engine_type, endpoint_url, auth_ref,
                   external_job_id, connector_name, deployment_ref, enabled, owner_name,
                   max_staleness_seconds, config_json, last_probed_at, last_probe_status,
                   last_probe_message, created_at, updated_at
            FROM cdp_warehouse_external_realtime_job_probe_target
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
    CdpWarehouseExternalRealtimeJobProbeTargetDO findByKey(@Param("tenantId") Long tenantId,
                                                           @Param("pipelineKey") String pipelineKey,
                                                           @Param("jobKey") String jobKey);

    @Select("""
            SELECT id, tenant_id, pipeline_key, job_key, engine_type, endpoint_url, auth_ref,
                   external_job_id, connector_name, deployment_ref, enabled, owner_name,
                   max_staleness_seconds, config_json, last_probed_at, last_probe_status,
                   last_probe_message, created_at, updated_at
            FROM cdp_warehouse_external_realtime_job_probe_target
            WHERE tenant_id = #{tenantId}
              AND id = #{targetId}
            LIMIT 1
            """)
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param targetId 业务对象 ID，用于定位具体记录。
     * @return 返回符合条件的数据列表或视图。
     */
    CdpWarehouseExternalRealtimeJobProbeTargetDO findByTenantAndId(@Param("tenantId") Long tenantId,
                                                                   @Param("targetId") Long targetId);

    @Select("""
            SELECT id, tenant_id, pipeline_key, job_key, engine_type, endpoint_url, auth_ref,
                   external_job_id, connector_name, deployment_ref, enabled, owner_name,
                   max_staleness_seconds, config_json, last_probed_at, last_probe_status,
                   last_probe_message, created_at, updated_at
            FROM cdp_warehouse_external_realtime_job_probe_target
            WHERE tenant_id = #{tenantId}
            ORDER BY pipeline_key ASC, job_key ASC, id ASC
            LIMIT #{limit}
            """)
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    List<CdpWarehouseExternalRealtimeJobProbeTargetDO> listTargets(@Param("tenantId") Long tenantId,
                                                                   @Param("limit") int limit);

    @Select("""
            SELECT id, tenant_id, pipeline_key, job_key, engine_type, endpoint_url, auth_ref,
                   external_job_id, connector_name, deployment_ref, enabled, owner_name,
                   max_staleness_seconds, config_json, last_probed_at, last_probe_status,
                   last_probe_message, created_at, updated_at
            FROM cdp_warehouse_external_realtime_job_probe_target
            WHERE tenant_id = #{tenantId}
              AND enabled = 1
            ORDER BY pipeline_key ASC, job_key ASC, id ASC
            LIMIT #{limit}
            """)
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    List<CdpWarehouseExternalRealtimeJobProbeTargetDO> listEnabledTargets(@Param("tenantId") Long tenantId,
                                                                          @Param("limit") int limit);

    @Update("""
            UPDATE cdp_warehouse_external_realtime_job_probe_target
            SET enabled = #{enabled},
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND id = #{targetId}
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param targetId 业务对象 ID，用于定位具体记录。
     * @param enabled enabled 参数，用于 updateEnabled 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    int updateEnabled(@Param("tenantId") Long tenantId,
                      @Param("targetId") Long targetId,
                      @Param("enabled") int enabled);

    @Update("""
            UPDATE cdp_warehouse_external_realtime_job_probe_target
            SET last_probed_at = #{lastProbedAt},
                last_probe_status = #{lastProbeStatus},
                last_probe_message = #{lastProbeMessage},
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND id = #{targetId}
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param targetId 业务对象 ID，用于定位具体记录。
     * @param lastProbedAt 时间参数，用于计算窗口、过期或审计时间。
     * @param lastProbeStatus 业务状态，用于筛选或推进状态流转。
     * @param lastProbeMessage last probe message 参数，用于 updateProbeResult 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    int updateProbeResult(@Param("tenantId") Long tenantId,
                          @Param("targetId") Long targetId,
                          @Param("lastProbedAt") LocalDateTime lastProbedAt,
                          @Param("lastProbeStatus") String lastProbeStatus,
                          @Param("lastProbeMessage") String lastProbeMessage);
}
