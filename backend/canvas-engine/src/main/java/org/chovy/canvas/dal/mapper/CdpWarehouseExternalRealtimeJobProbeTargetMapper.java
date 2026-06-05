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
    List<CdpWarehouseExternalRealtimeJobProbeTargetDO> listEnabledTargets(@Param("tenantId") Long tenantId,
                                                                          @Param("limit") int limit);

    @Update("""
            UPDATE cdp_warehouse_external_realtime_job_probe_target
            SET enabled = #{enabled},
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND id = #{targetId}
            """)
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
    int updateProbeResult(@Param("tenantId") Long tenantId,
                          @Param("targetId") Long targetId,
                          @Param("lastProbedAt") LocalDateTime lastProbedAt,
                          @Param("lastProbeStatus") String lastProbeStatus,
                          @Param("lastProbeMessage") String lastProbeMessage);
}
