package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.BiResourceCommentDO;

import java.time.LocalDateTime;

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
    int softDelete(@Param("tenantId") Long tenantId,
                   @Param("workspaceId") Long workspaceId,
                   @Param("commentId") Long commentId,
                   @Param("username") String username,
                   @Param("deletedAt") LocalDateTime deletedAt);
}
