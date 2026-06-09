package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.chovy.canvas.dal.dataobject.AudienceMaterializationRunDO;

/**
 * AudienceMaterializationRunMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface AudienceMaterializationRunMapper extends BaseMapper<AudienceMaterializationRunDO> {

            /**
             * 执行 COALESCE 流程，围绕 coalesce 完成校验、计算或结果组装。
             *
             * @param audienceId 业务对象 ID，用于定位具体记录。
             * @return 返回 COALESCE 流程生成的业务结果。
             */
    @Select("""
            SELECT COALESCE(MAX(version), 0) + 1
            FROM audience_materialization_run
            WHERE tenant_id = #{tenantId}
              AND audience_id = #{audienceId}
            """)
    /**
     * 执行 nextVersion 流程，围绕 next version 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @return 返回 next version 计算得到的数量、金额或指标值。
     */
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
    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @return 返回 latestSuccessfulRun 流程生成的业务结果。
     */
    AudienceMaterializationRunDO latestSuccessfulRun(@Param("tenantId") Long tenantId,
                                                     @Param("audienceId") Long audienceId);
}
