package org.chovy.canvas.bi.domain;

import java.util.List;
/**
 * BiDashboardRepository 仓储接口。
 */
public interface BiDashboardRepository {
    /**
     * 执行 find Dashboard By Key 相关处理。
     */

    BiDashboard findDashboardByKey(Long tenantId, Long workspaceId, BiResourceKey dashboardKey);
    /**
     * 查询列表数据。
     */

    List<BiDashboard> listAvailableDashboards(Long tenantId);
    /**
     * 执行 find Available Dashboard By Key 相关处理。
     */

    BiDashboard findAvailableDashboardByKey(Long tenantId, BiResourceKey dashboardKey);
    /**
     * 执行 save Dashboard 相关处理。
     */

    BiDashboard saveDashboard(BiDashboard dashboard);
}
