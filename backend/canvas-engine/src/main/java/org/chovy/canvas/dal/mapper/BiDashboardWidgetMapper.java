package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.chovy.canvas.dal.dataobject.BiDashboardWidgetDO;

/**
 * BiDashboardWidgetMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface BiDashboardWidgetMapper extends BaseMapper<BiDashboardWidgetDO> {

    @Delete("""
            DELETE FROM bi_dashboard_widget
            WHERE tenant_id = #{tenantId}
              AND dashboard_id = #{dashboardId}
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param dashboardId 业务对象 ID，用于定位具体记录。
     * @return 返回 delete by dashboard 计算得到的数量、金额或指标值。
     */
    int deleteByDashboard(@Param("tenantId") Long tenantId, @Param("dashboardId") Long dashboardId);
}
