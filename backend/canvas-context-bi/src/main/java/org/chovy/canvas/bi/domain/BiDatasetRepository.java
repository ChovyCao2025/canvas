package org.chovy.canvas.bi.domain;

import java.util.List;
/**
 * BiDatasetRepository 仓储接口。
 */
public interface BiDatasetRepository {
    /**
     * 执行 find Dataset By Key 相关处理。
     */

    BiDataset findDatasetByKey(Long tenantId, Long workspaceId, BiResourceKey datasetKey);
    /**
     * 执行 find Dataset By Id 相关处理。
     */

    BiDataset findDatasetById(Long tenantId, Long datasetId);
    /**
     * 查询列表数据。
     */

    List<BiDataset> listAvailableDatasets(Long tenantId);
    /**
     * 执行 find Available Dataset By Key With Tenant Fallback 相关处理。
     */

    BiDataset findAvailableDatasetByKeyWithTenantFallback(Long tenantId, BiResourceKey datasetKey);
    /**
     * 执行 save Dataset 相关处理。
     */

    BiDataset saveDataset(BiDataset dataset);
}
