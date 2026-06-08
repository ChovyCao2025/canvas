package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.BiResourceCommentDO;

import java.time.LocalDateTime;

/**
 * BiResourceCommentMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface BiResourceCommentMapper extends BaseMapper<BiResourceCommentDO> {

    @Update("""
            UPDATE bi_resource_comment
            SET deleted_at = #{deletedAt}
            WHERE tenant_id = #{tenantId}
              AND workspace_id = #{workspaceId}
              AND id = #{commentId}
              AND created_by = #{username}
              AND deleted_at IS NULL
            """)
    /**
     * 执行 softDelete 流程，围绕 soft delete 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param commentId 业务对象 ID，用于定位具体记录。
     * @param username 操作人标识，用于审计和权限判断。
     * @param deletedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 soft delete 计算得到的数量、金额或指标值。
     */
    int softDelete(@Param("tenantId") Long tenantId,
                   @Param("workspaceId") Long workspaceId,
                   @Param("commentId") Long commentId,
                   @Param("username") String username,
                   @Param("deletedAt") LocalDateTime deletedAt);
}
