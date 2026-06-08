package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.chovy.canvas.dal.dataobject.BiResourceFavoriteDO;

/**
 * BiResourceFavoriteMapper 定义 dal.mapper 场景中的扩展契约。
 */
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
    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回流程执行后的业务结果。
     */
    int upsert(@Param("row") BiResourceFavoriteDO row);

    @Delete("""
            DELETE FROM bi_resource_favorite
            WHERE tenant_id = #{tenantId}
              AND workspace_id = #{workspaceId}
              AND resource_type = #{resourceType}
              AND resource_key = #{resourceKey}
              AND username = #{username}
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceKey 业务键，用于在同一租户下定位资源。
     * @param username 操作人标识，用于审计和权限判断。
     * @return 返回 delete favorite 计算得到的数量、金额或指标值。
     */
    int deleteFavorite(@Param("tenantId") Long tenantId,
                       @Param("workspaceId") Long workspaceId,
                       @Param("resourceType") String resourceType,
                       @Param("resourceKey") String resourceKey,
                       @Param("username") String username);
}
