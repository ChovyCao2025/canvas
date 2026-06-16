package org.chovy.canvas.bi.domain;

import java.util.List;
/**
 * BiChartRepository 仓储接口。
 */
public interface BiChartRepository {
    /**
     * 执行 find Chart By Key 相关处理。
     */

    BiChart findChartByKey(Long tenantId, Long workspaceId, BiResourceKey chartKey);
    /**
     * 查询列表数据。
     */

    List<BiChart> listChartsByKeys(Long tenantId, Long workspaceId, List<BiResourceKey> chartKeys);
    /**
     * 查询列表数据。
     */

    List<BiChart> listAvailableCharts(Long tenantId);
    /**
     * 执行 find Available Chart By Key 相关处理。
     */

    BiChart findAvailableChartByKey(Long tenantId, BiResourceKey chartKey);
    /**
     * 执行 save Chart 相关处理。
     */

    BiChart saveChart(BiChart chart);
}
