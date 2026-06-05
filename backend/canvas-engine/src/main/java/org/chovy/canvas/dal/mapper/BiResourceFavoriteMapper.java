package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.chovy.canvas.dal.dataobject.BiResourceFavoriteDO;

@Mapper
public interface BiResourceFavoriteMapper extends BaseMapper<BiResourceFavoriteDO> {

    @Insert("""
            INSERT INTO bi_resource_favorite
                (tenant_id, workspace_id, resource_type, resource_key, username, created_at)
            VALUES
                (#{row.tenantId}, #{row.workspaceId}, #{row.resourceType}, #{row.resourceKey},
                 #{row.username}, #{row.createdAt})
            ON DUPLICATE KEY UPDATE
                created_at = VALUES(created_at)
            """)
    int upsert(@Param("row") BiResourceFavoriteDO row);

    @Delete("""
            DELETE FROM bi_resource_favorite
            WHERE tenant_id = #{tenantId}
              AND workspace_id = #{workspaceId}
              AND resource_type = #{resourceType}
              AND resource_key = #{resourceKey}
              AND username = #{username}
            """)
    int deleteFavorite(@Param("tenantId") Long tenantId,
                       @Param("workspaceId") Long workspaceId,
                       @Param("resourceType") String resourceType,
                       @Param("resourceKey") String resourceKey,
                       @Param("username") String username);
}
