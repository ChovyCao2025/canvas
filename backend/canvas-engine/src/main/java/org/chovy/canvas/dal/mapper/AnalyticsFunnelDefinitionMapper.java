package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.chovy.canvas.dal.dataobject.AnalyticsFunnelDefinitionDO;

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
    AnalyticsFunnelDefinitionDO selectLatestEnabled(@Param("tenantId") Long tenantId,
                                                    @Param("funnelKey") String funnelKey);
}
