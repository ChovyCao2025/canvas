package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.chovy.canvas.dal.dataobject.AnalyticsFunnelDefinitionDO;

/**
 * AnalyticsFunnelDefinitionMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface AnalyticsFunnelDefinitionMapper extends BaseMapper<AnalyticsFunnelDefinitionDO> {

    @Select("""
            SELECT *
            FROM analytics_funnel_definition
            WHERE tenant_id = #{tenantId}
              AND funnel_key = #{funnelKey}
              AND enabled = 1
            ORDER BY version DESC
            LIMIT 1
            """)
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param funnelKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    AnalyticsFunnelDefinitionDO selectLatestEnabled(@Param("tenantId") Long tenantId,
                                                    @Param("funnelKey") String funnelKey);
}
