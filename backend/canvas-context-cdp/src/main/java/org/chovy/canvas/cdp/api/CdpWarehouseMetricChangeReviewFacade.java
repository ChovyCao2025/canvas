package org.chovy.canvas.cdp.api;

import java.util.List;
import java.util.Map;

public interface CdpWarehouseMetricChangeReviewFacade {

    List<Map<String, Object>> list(Long tenantId, String datasetKey, String metricKey, String status);

    Map<String, Object> create(Long tenantId, String username, MetricChangeCommand command);

    Map<String, Object> approve(Long tenantId, String username, Long reviewId, String reviewNote);

    Map<String, Object> reject(Long tenantId, String username, Long reviewId, String reviewNote);

    Map<String, Object> apply(Long tenantId, String username, Long reviewId);

    record MetricChangeCommand(
            String datasetKey,
            String metricKey,
            String proposedExpression,
            List<String> proposedAllowedDimensions,
            String reason) {
        public MetricChangeCommand {
            proposedAllowedDimensions = proposedAllowedDimensions == null
                    ? List.of()
                    : List.copyOf(proposedAllowedDimensions);
        }
    }
}
