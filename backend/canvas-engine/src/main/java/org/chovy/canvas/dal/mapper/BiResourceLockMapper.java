package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.chovy.canvas.dal.dataobject.BiResourceLockDO;

@Mapper
public interface BiResourceLockMapper extends BaseMapper<BiResourceLockDO> {

    @Insert("""
            INSERT INTO bi_resource_lock
                (tenant_id, workspace_id, resource_type, resource_key, lock_token, locked_by, locked_at, expires_at)
            VALUES
                (#{row.tenantId}, #{row.workspaceId}, #{row.resourceType}, #{row.resourceKey},
                 #{row.lockToken}, #{row.lockedBy}, #{row.lockedAt}, #{row.expiresAt})
            ON DUPLICATE KEY UPDATE
                lock_token = IF(expires_at <= VALUES(locked_at) OR locked_by = VALUES(locked_by),
                    VALUES(lock_token), lock_token),
                locked_by = IF(expires_at <= VALUES(locked_at) OR locked_by = VALUES(locked_by),
                    VALUES(locked_by), locked_by),
                locked_at = IF(expires_at <= VALUES(locked_at) OR locked_by = VALUES(locked_by),
                    VALUES(locked_at), locked_at),
                expires_at = IF(expires_at <= VALUES(locked_at) OR locked_by = VALUES(locked_by),
                    VALUES(expires_at), expires_at)
            """)
    int acquire(@Param("row") BiResourceLockDO row);

    @Select("""
            SELECT id, tenant_id, workspace_id, resource_type, resource_key,
                   lock_token, locked_by, locked_at, expires_at
            FROM bi_resource_lock
            WHERE tenant_id = #{tenantId}
              AND workspace_id = #{workspaceId}
              AND resource_type = #{resourceType}
              AND resource_key = #{resourceKey}
            LIMIT 1
            """)
    BiResourceLockDO selectCurrent(@Param("tenantId") Long tenantId,
                                   @Param("workspaceId") Long workspaceId,
                                   @Param("resourceType") String resourceType,
                                   @Param("resourceKey") String resourceKey);

    @Delete("""
            DELETE FROM bi_resource_lock
            WHERE tenant_id = #{tenantId}
              AND workspace_id = #{workspaceId}
              AND resource_type = #{resourceType}
              AND resource_key = #{resourceKey}
              AND lock_token = #{lockToken}
              AND locked_by = #{username}
            """)
    int release(@Param("tenantId") Long tenantId,
                @Param("workspaceId") Long workspaceId,
                @Param("resourceType") String resourceType,
                @Param("resourceKey") String resourceKey,
                @Param("lockToken") String lockToken,
                @Param("username") String username);
}
