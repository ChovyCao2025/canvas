package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.BiPortalDO;

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
    int upsert(@Param("row") BiPortalDO row);

    @Update("""
            UPDATE bi_portal
            SET status = 'PUBLISHED',
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND workspace_id = #{workspaceId}
              AND portal_key = #{portalKey}
            """)
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
    int archive(@Param("tenantId") Long tenantId,
                @Param("workspaceId") Long workspaceId,
                @Param("portalKey") String portalKey);
}
