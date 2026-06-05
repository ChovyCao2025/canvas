package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.chovy.canvas.dal.dataobject.CdpWarehouseStreamSchemaDO;

import java.util.List;

@Mapper
public interface CdpWarehouseStreamSchemaMapper extends BaseMapper<CdpWarehouseStreamSchemaDO> {

    @Insert("""
            INSERT INTO cdp_warehouse_stream_schema
            (tenant_id, pipeline_key, schema_role, schema_version, schema_hash, schema_json,
             compatibility_policy, compatibility_status, compatibility_reason, active, registered_by)
            VALUES
            (#{row.tenantId}, #{row.pipelineKey}, #{row.schemaRole}, #{row.schemaVersion}, #{row.schemaHash},
             #{row.schemaJson}, #{row.compatibilityPolicy}, #{row.compatibilityStatus},
             #{row.compatibilityReason}, #{row.active}, #{row.registeredBy})
            ON DUPLICATE KEY UPDATE
                schema_hash = VALUES(schema_hash),
                schema_json = VALUES(schema_json),
                compatibility_policy = VALUES(compatibility_policy),
                compatibility_status = VALUES(compatibility_status),
                compatibility_reason = VALUES(compatibility_reason),
                active = VALUES(active),
                registered_by = VALUES(registered_by),
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsert(@Param("row") CdpWarehouseStreamSchemaDO row);

    @Select("""
            SELECT id, tenant_id, pipeline_key, schema_role, schema_version, schema_hash, schema_json,
                   compatibility_policy, compatibility_status, compatibility_reason, active, registered_by,
                   created_at, updated_at
            FROM cdp_warehouse_stream_schema
            WHERE tenant_id = #{tenantId}
              AND pipeline_key = #{pipelineKey}
              AND schema_role = #{schemaRole}
              AND schema_version = #{schemaVersion}
              AND active = 1
            LIMIT 1
            """)
    CdpWarehouseStreamSchemaDO findActiveVersion(@Param("tenantId") Long tenantId,
                                                 @Param("pipelineKey") String pipelineKey,
                                                 @Param("schemaRole") String schemaRole,
                                                 @Param("schemaVersion") String schemaVersion);

    @Select("""
            SELECT id, tenant_id, pipeline_key, schema_role, schema_version, schema_hash, schema_json,
                   compatibility_policy, compatibility_status, compatibility_reason, active, registered_by,
                   created_at, updated_at
            FROM cdp_warehouse_stream_schema
            WHERE tenant_id = #{tenantId}
              AND pipeline_key = #{pipelineKey}
              AND schema_role = #{schemaRole}
              AND active = 1
            ORDER BY created_at DESC, id DESC
            LIMIT 1
            """)
    CdpWarehouseStreamSchemaDO latestActive(@Param("tenantId") Long tenantId,
                                            @Param("pipelineKey") String pipelineKey,
                                            @Param("schemaRole") String schemaRole);

    @Select("""
            SELECT id, tenant_id, pipeline_key, schema_role, schema_version, schema_hash, schema_json,
                   compatibility_policy, compatibility_status, compatibility_reason, active, registered_by,
                   created_at, updated_at
            FROM cdp_warehouse_stream_schema
            WHERE tenant_id = #{tenantId}
              AND pipeline_key = #{pipelineKey}
              AND schema_role = #{schemaRole}
              AND active = 1
              AND id <> #{excludeId}
            ORDER BY created_at DESC, id DESC
            LIMIT 1
            """)
    CdpWarehouseStreamSchemaDO latestActiveExcluding(@Param("tenantId") Long tenantId,
                                                     @Param("pipelineKey") String pipelineKey,
                                                     @Param("schemaRole") String schemaRole,
                                                     @Param("excludeId") Long excludeId);

    @Select("""
            SELECT id, tenant_id, pipeline_key, schema_role, schema_version, schema_hash, schema_json,
                   compatibility_policy, compatibility_status, compatibility_reason, active, registered_by,
                   created_at, updated_at
            FROM cdp_warehouse_stream_schema
            WHERE tenant_id = #{tenantId}
              AND pipeline_key = #{pipelineKey}
              AND (#{schemaRole} IS NULL OR schema_role = #{schemaRole})
            ORDER BY schema_role ASC, created_at DESC, id DESC
            LIMIT #{limit}
            """)
    List<CdpWarehouseStreamSchemaDO> listVersions(@Param("tenantId") Long tenantId,
                                                  @Param("pipelineKey") String pipelineKey,
                                                  @Param("schemaRole") String schemaRole,
                                                  @Param("limit") int limit);
}
