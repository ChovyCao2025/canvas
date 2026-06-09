package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.chovy.canvas.dal.dataobject.CdpWarehouseStreamSchemaDO;

import java.util.List;

/**
 * CdpWarehouseStreamSchemaMapper 定义 dal.mapper 场景中的扩展契约。
 */
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
    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回流程执行后的业务结果。
     */
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
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param pipelineKey 业务键，用于在同一租户下定位资源。
     * @param schemaRole schema role 参数，用于 findActiveVersion 流程中的校验、计算或对象转换。
     * @param schemaVersion schema version 参数，用于 findActiveVersion 流程中的校验、计算或对象转换。
     * @return 返回符合条件的数据列表或视图。
     */
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
    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param pipelineKey 业务键，用于在同一租户下定位资源。
     * @param schemaRole schema role 参数，用于 latestActive 流程中的校验、计算或对象转换。
     * @return 返回 latestActive 流程生成的业务结果。
     */
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
    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param pipelineKey 业务键，用于在同一租户下定位资源。
     * @param schemaRole schema role 参数，用于 latestActiveExcluding 流程中的校验、计算或对象转换。
     * @param excludeId 业务对象 ID，用于定位具体记录。
     * @return 返回 latestActiveExcluding 流程生成的业务结果。
     */
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
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param pipelineKey 业务键，用于在同一租户下定位资源。
     * @param schemaRole schema role 参数，用于 listVersions 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    List<CdpWarehouseStreamSchemaDO> listVersions(@Param("tenantId") Long tenantId,
                                                  @Param("pipelineKey") String pipelineKey,
                                                  @Param("schemaRole") String schemaRole,
                                                  @Param("limit") int limit);
}
