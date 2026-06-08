package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.BiPortalDO;

/**
 * BiPortalMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface BiPortalMapper extends BaseMapper<BiPortalDO> {

    @Insert("""
            INSERT INTO bi_portal
                (tenant_id, workspace_id, portal_key, name, theme_json, status, created_by)
            VALUES
                (#{row.tenantId}, #{row.workspaceId}, #{row.portalKey}, #{row.name},
                 #{row.themeJson}, #{row.status}, #{row.createdBy})
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                theme_json = VALUES(theme_json),
                status = VALUES(status),
                updated_at = CURRENT_TIMESTAMP
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回流程执行后的业务结果。
     */
    int upsert(@Param("row") BiPortalDO row);

    @Update("""
            UPDATE bi_portal
            SET status = 'PUBLISHED',
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND workspace_id = #{workspaceId}
              AND portal_key = #{portalKey}
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param portalKey 业务键，用于在同一租户下定位资源。
     * @return 返回流程执行后的业务结果。
     */
    int publish(@Param("tenantId") Long tenantId,
                @Param("workspaceId") Long workspaceId,
                @Param("portalKey") String portalKey);

    @Update("""
            UPDATE bi_portal
            SET status = 'ARCHIVED',
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND workspace_id = #{workspaceId}
              AND portal_key = #{portalKey}
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param portalKey 业务键，用于在同一租户下定位资源。
     * @return 返回 archive 计算得到的数量、金额或指标值。
     */
    int archive(@Param("tenantId") Long tenantId,
                @Param("workspaceId") Long workspaceId,
                @Param("portalKey") String portalKey);
}
