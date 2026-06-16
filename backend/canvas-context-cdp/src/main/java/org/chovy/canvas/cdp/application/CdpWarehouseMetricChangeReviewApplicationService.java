package org.chovy.canvas.cdp.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseMetricChangeReviewFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseMetricChangeReviewCatalog;
import org.springframework.stereotype.Service;

/**
 * 编排 CdpWarehouseMetricChangeReview 的应用服务流程。
 */
@Service
public class CdpWarehouseMetricChangeReviewApplicationService implements CdpWarehouseMetricChangeReviewFacade {

    /**
     * 领域目录组件。
     */
    private final CdpWarehouseMetricChangeReviewCatalog catalog;

    /**
     * 创建当前组件实例。
     */
    public CdpWarehouseMetricChangeReviewApplicationService() {
        this(new CdpWarehouseMetricChangeReviewCatalog());
    }

    CdpWarehouseMetricChangeReviewApplicationService(CdpWarehouseMetricChangeReviewCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 查询list列表。
     */
    @Override
    public List<Map<String, Object>> list(Long tenantId, String datasetKey, String metricKey, String status) {
        return catalog.list(tenantIdOrDefault(tenantId), datasetKey, metricKey, status);
    }

    /**
     * 创建create。
     */
    @Override
    public Map<String, Object> create(Long tenantId, String username, MetricChangeCommand command) {
        return catalog.create(tenantIdOrDefault(tenantId), defaultActor(username), command);
    }

    /**
     * 执行 approve 对应的 CDP 业务操作。
     */
    @Override
    public Map<String, Object> approve(Long tenantId, String username, Long reviewId, String reviewNote) {
        return catalog.approve(tenantIdOrDefault(tenantId), defaultActor(username), reviewId, reviewNote);
    }

    /**
     * 执行 reject 对应的 CDP 业务操作。
     */
    @Override
    public Map<String, Object> reject(Long tenantId, String username, Long reviewId, String reviewNote) {
        return catalog.reject(tenantIdOrDefault(tenantId), defaultActor(username), reviewId, reviewNote);
    }

    /**
     * 执行 apply 对应的 CDP 业务操作。
     */
    @Override
    public Map<String, Object> apply(Long tenantId, String username, Long reviewId) {
        return catalog.apply(tenantIdOrDefault(tenantId), reviewId);
    }

    /**
     * 执行 tenantIdOrDefault 对应的 CDP 业务操作。
     */
    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 返回默认的Actor。
     */
    private static String defaultActor(String username) {
        return username == null || username.isBlank() ? "system" : username.trim();
    }
}
