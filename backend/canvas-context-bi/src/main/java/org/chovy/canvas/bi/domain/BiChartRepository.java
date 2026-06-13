package org.chovy.canvas.bi.domain;

import java.util.List;

public interface BiChartRepository {

    BiChart findChartByKey(Long tenantId, Long workspaceId, BiResourceKey chartKey);

    List<BiChart> listChartsByKeys(Long tenantId, Long workspaceId, List<BiResourceKey> chartKeys);

    List<BiChart> listAvailableCharts(Long tenantId);

    BiChart findAvailableChartByKey(Long tenantId, BiResourceKey chartKey);

    BiChart saveChart(BiChart chart);
}
