package org.chovy.canvas.cdp.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseMetricChangeReviewFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseMetricChangeReviewCatalog;
import org.springframework.stereotype.Service;

@Service
public class CdpWarehouseMetricChangeReviewApplicationService implements CdpWarehouseMetricChangeReviewFacade {

    private final CdpWarehouseMetricChangeReviewCatalog catalog;

    public CdpWarehouseMetricChangeReviewApplicationService() {
        this(new CdpWarehouseMetricChangeReviewCatalog());
    }

    CdpWarehouseMetricChangeReviewApplicationService(CdpWarehouseMetricChangeReviewCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public List<Map<String, Object>> list(Long tenantId, String datasetKey, String metricKey, String status) {
        return catalog.list(tenantIdOrDefault(tenantId), datasetKey, metricKey, status);
    }

    @Override
    public Map<String, Object> create(Long tenantId, String username, MetricChangeCommand command) {
        return catalog.create(tenantIdOrDefault(tenantId), defaultActor(username), command);
    }

    @Override
    public Map<String, Object> approve(Long tenantId, String username, Long reviewId, String reviewNote) {
        return catalog.approve(tenantIdOrDefault(tenantId), defaultActor(username), reviewId, reviewNote);
    }

    @Override
    public Map<String, Object> reject(Long tenantId, String username, Long reviewId, String reviewNote) {
        return catalog.reject(tenantIdOrDefault(tenantId), defaultActor(username), reviewId, reviewNote);
    }

    @Override
    public Map<String, Object> apply(Long tenantId, String username, Long reviewId) {
        return catalog.apply(tenantIdOrDefault(tenantId), reviewId);
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private static String defaultActor(String username) {
        return username == null || username.isBlank() ? "system" : username.trim();
    }
}
