package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;

/**
 * BiDatasetMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface BiDatasetMapper extends BaseMapper<BiDatasetDO> {

    @Insert("""
            INSERT INTO bi_dataset
                (tenant_id, workspace_id, dataset_key, name, dataset_type, source_ref_id,
                 table_expression, tenant_column, model_json, status, created_by)
            VALUES
                (#{row.tenantId}, #{row.workspaceId}, #{row.datasetKey}, #{row.name},
                 #{row.datasetType}, #{row.sourceRefId}, #{row.tableExpression},
                 #{row.tenantColumn}, #{row.modelJson}, #{row.status}, #{row.createdBy})
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                dataset_type = VALUES(dataset_type),
                source_ref_id = VALUES(source_ref_id),
                table_expression = VALUES(table_expression),
                tenant_column = VALUES(tenant_column),
                model_json = VALUES(model_json),
                status = VALUES(status),
                updated_at = CURRENT_TIMESTAMP
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回流程执行后的业务结果。
     */
    int upsert(@Param("row") BiDatasetDO row);

    @Update("""
            UPDATE bi_dataset
            SET status = 'PUBLISHED',
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND workspace_id = #{workspaceId}
              AND dataset_key = #{datasetKey}
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @return 返回流程执行后的业务结果。
     */
    int publish(@Param("tenantId") Long tenantId,
                @Param("workspaceId") Long workspaceId,
                @Param("datasetKey") String datasetKey);

    @Update("""
            UPDATE bi_dataset
            SET status = 'ARCHIVED',
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND workspace_id = #{workspaceId}
              AND dataset_key = #{datasetKey}
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @return 返回 archive 计算得到的数量、金额或指标值。
     */
    int archive(@Param("tenantId") Long tenantId,
                @Param("workspaceId") Long workspaceId,
                @Param("datasetKey") String datasetKey);
}
