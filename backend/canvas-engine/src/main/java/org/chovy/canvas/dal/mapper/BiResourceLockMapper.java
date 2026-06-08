package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.chovy.canvas.dal.dataobject.BiResourceLockDO;

/**
 * BiResourceLockMapper 定义 dal.mapper 场景中的扩展契约。
 */
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
    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 acquire 计算得到的数量、金额或指标值。
     */
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
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
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
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceKey 业务键，用于在同一租户下定位资源。
     * @param lockToken 令牌或锁标识，用于鉴权、幂等或并发控制。
     * @param username 操作人标识，用于审计和权限判断。
     * @return 返回 release 计算得到的数量、金额或指标值。
     */
    int release(@Param("tenantId") Long tenantId,
                @Param("workspaceId") Long workspaceId,
                @Param("resourceType") String resourceType,
                @Param("resourceKey") String resourceKey,
                @Param("lockToken") String lockToken,
                @Param("username") String username);
}
