package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.BiDashboardDO;

/**
 * BiDashboardMapper 定义 dal.mapper 场景中的扩展契约。
 */
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
    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回流程执行后的业务结果。
     */
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
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param dashboardKey 业务键，用于在同一租户下定位资源。
     * @return 返回流程执行后的业务结果。
     */
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
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param dashboardKey 业务键，用于在同一租户下定位资源。
     * @return 返回 archive 计算得到的数量、金额或指标值。
     */
    int archive(@Param("tenantId") Long tenantId,
                @Param("workspaceId") Long workspaceId,
                @Param("dashboardKey") String dashboardKey);
}
