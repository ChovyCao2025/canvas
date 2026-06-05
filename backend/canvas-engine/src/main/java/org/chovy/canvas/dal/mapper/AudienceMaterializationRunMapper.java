package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.chovy.canvas.dal.dataobject.AudienceMaterializationRunDO;

@Mapper
public interface AudienceMaterializationRunMapper extends BaseMapper<AudienceMaterializationRunDO> {

    @Select("""
            SELECT COALESCE(MAX(version), 0) + 1
            FROM audience_materialization_run
            WHERE tenant_id = #{tenantId}
              AND audience_id = #{audienceId}
            """)
    Long nextVersion(@Param("tenantId") Long tenantId,
                     @Param("audienceId") Long audienceId);

    @Select("""
            SELECT id, tenant_id, audience_id, version, status, rule_json, matched_users,
                   bitmap_key, error_message, started_at, finished_at, created_by
            FROM audience_materialization_run
            WHERE tenant_id = #{tenantId}
              AND audience_id = #{audienceId}
              AND status = 'SUCCESS'
            ORDER BY finished_at DESC, id DESC
            LIMIT 1
            """)
    AudienceMaterializationRunDO latestSuccessfulRun(@Param("tenantId") Long tenantId,
                                                     @Param("audienceId") Long audienceId);
}
