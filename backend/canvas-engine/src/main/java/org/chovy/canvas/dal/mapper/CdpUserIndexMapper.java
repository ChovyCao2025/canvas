package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.chovy.canvas.dal.dataobject.CdpUserIndexDO;

/**
 * CdpUserIndexMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface CdpUserIndexMapper extends BaseMapper<CdpUserIndexDO> {

    @Select("""
            SELECT id, tenant_id, user_id, user_index, created_at, updated_at
            FROM cdp_user_index
            WHERE tenant_id = #{tenantId}
              AND user_id = #{userId}
            LIMIT 1
            """)
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @return 返回符合条件的数据列表或视图。
     */
    CdpUserIndexDO selectByTenantAndUser(@Param("tenantId") Long tenantId,
                                         @Param("userId") String userId);

            /**
             * 执行 COALESCE 流程，围绕 coalesce 完成校验、计算或结果组装。
             *
             * @param tenantId 租户 ID，用于限定数据隔离范围。
             * @return 返回 COALESCE 流程生成的业务结果。
             */
    @Select("""
            SELECT COALESCE(MAX(user_index), 0) + 1
            FROM cdp_user_index
            WHERE tenant_id = #{tenantId}
            """)
    /**
     * 执行 nextIndexForTenant 流程，围绕 next index for tenant 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 next index for tenant 计算得到的数量、金额或指标值。
     */
    Long nextIndexForTenant(@Param("tenantId") Long tenantId);
}
