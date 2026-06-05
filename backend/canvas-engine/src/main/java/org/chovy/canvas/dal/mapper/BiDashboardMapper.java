package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.BiDashboardDO;

@Mapper
public interface BiDashboardMapper extends BaseMapper<BiDashboardDO> {

    @Insert("""
            INSERT INTO bi_dashboard
                (tenant_id, workspace_id, dashboard_key, name, description, theme_json,
                 filter_json, status, version, created_by)
            VALUES
                (#{row.tenantId}, #{row.workspaceId}, #{row.dashboardKey}, #{row.name},
                 #{row.description}, #{row.themeJson}, #{row.filterJson}, #{row.status},
                 #{row.version}, #{row.createdBy})
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                description = VALUES(description),
                theme_json = VALUES(theme_json),
                filter_json = VALUES(filter_json),
                status = VALUES(status),
                version = VALUES(version),
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsert(@Param("row") BiDashboardDO row);

    @Update("""
            UPDATE bi_dashboard
            SET status = 'PUBLISHED',
                version = version + 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND workspace_id = #{workspaceId}
              AND dashboard_key = #{dashboardKey}
            """)
    int publish(@Param("tenantId") Long tenantId,
                @Param("workspaceId") Long workspaceId,
                @Param("dashboardKey") String dashboardKey);

    @Update("""
            UPDATE bi_dashboard
            SET status = 'ARCHIVED',
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND workspace_id = #{workspaceId}
              AND dashboard_key = #{dashboardKey}
            """)
    int archive(@Param("tenantId") Long tenantId,
                @Param("workspaceId") Long workspaceId,
                @Param("dashboardKey") String dashboardKey);
}
