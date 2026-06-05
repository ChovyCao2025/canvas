package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.chovy.canvas.dal.dataobject.BiDashboardWidgetDO;

@Mapper
public interface BiDashboardWidgetMapper extends BaseMapper<BiDashboardWidgetDO> {

    @Delete("""
            DELETE FROM bi_dashboard_widget
            WHERE tenant_id = #{tenantId}
              AND dashboard_id = #{dashboardId}
            """)
    int deleteByDashboard(@Param("tenantId") Long tenantId, @Param("dashboardId") Long dashboardId);
}
