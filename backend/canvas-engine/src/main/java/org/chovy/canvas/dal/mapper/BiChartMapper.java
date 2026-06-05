package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.BiChartDO;

@Mapper
public interface BiChartMapper extends BaseMapper<BiChartDO> {

    @Insert("""
            INSERT INTO bi_chart
                (tenant_id, workspace_id, chart_key, name, chart_type, dataset_id,
                 query_json, style_json, interaction_json, status, created_by)
            VALUES
                (#{row.tenantId}, #{row.workspaceId}, #{row.chartKey}, #{row.name},
                 #{row.chartType}, #{row.datasetId}, #{row.queryJson}, #{row.styleJson},
                 #{row.interactionJson}, #{row.status}, #{row.createdBy})
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                chart_type = VALUES(chart_type),
                dataset_id = VALUES(dataset_id),
                query_json = VALUES(query_json),
                style_json = VALUES(style_json),
                interaction_json = VALUES(interaction_json),
                status = VALUES(status),
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsert(@Param("row") BiChartDO row);

    @Update("""
            UPDATE bi_chart
            SET status = 'PUBLISHED',
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND workspace_id = #{workspaceId}
              AND chart_key = #{chartKey}
            """)
    int publish(@Param("tenantId") Long tenantId,
                @Param("workspaceId") Long workspaceId,
                @Param("chartKey") String chartKey);

    @Update("""
            UPDATE bi_chart
            SET status = 'ARCHIVED',
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND workspace_id = #{workspaceId}
              AND chart_key = #{chartKey}
            """)
    int archive(@Param("tenantId") Long tenantId,
                @Param("workspaceId") Long workspaceId,
                @Param("chartKey") String chartKey);
}
