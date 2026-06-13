package org.chovy.canvas.bi.domain;

import java.util.List;

public interface BiDashboardRepository {

    BiDashboard findDashboardByKey(Long tenantId, Long workspaceId, BiResourceKey dashboardKey);

    List<BiDashboard> listAvailableDashboards(Long tenantId);

    BiDashboard findAvailableDashboardByKey(Long tenantId, BiResourceKey dashboardKey);

    BiDashboard saveDashboard(BiDashboard dashboard);
}
