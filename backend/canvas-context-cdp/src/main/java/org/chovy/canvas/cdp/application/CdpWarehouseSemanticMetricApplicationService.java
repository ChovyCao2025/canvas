package org.chovy.canvas.cdp.application;

import java.util.List;

import org.chovy.canvas.cdp.api.CdpWarehouseSemanticMetricFacade;
import org.springframework.stereotype.Service;

@Service
public class CdpWarehouseSemanticMetricApplicationService implements CdpWarehouseSemanticMetricFacade {

    @Override
    public List<SemanticMetricView> listMetrics(Long tenantId, String datasetKey) {
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        String scopedDatasetKey = datasetKey == null || datasetKey.isBlank()
                ? "canvas_daily_stats"
                : datasetKey.trim();
        return List.of(new SemanticMetricView(
                scopedTenantId,
                scopedDatasetKey,
                "success_rate",
                "SUM(success_count)",
                "PERCENT",
                List.of("stat_date", "canvas_id"),
                "ALLOW_LIST",
                "BI_DATASET_SPEC"));
    }
}
