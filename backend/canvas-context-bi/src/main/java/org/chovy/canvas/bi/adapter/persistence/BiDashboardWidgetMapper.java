package org.chovy.canvas.bi.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
/**
 * BiDashboardWidgetMapper MyBatis 映射器。
 */
@Mapper
public interface BiDashboardWidgetMapper extends BaseMapper<BiDashboardWidgetDO> {

    @Delete("""
            DELETE FROM bi_dashboard_widget
            WHERE tenant_id = #{tenantId}
              AND dashboard_id = #{dashboardId}
            """)
    /**
     * 删除业务数据。
     */
    int deleteByDashboard(@Param("tenantId") Long tenantId, @Param("dashboardId") Long dashboardId);
}
