package org.chovy.canvas.cdp.api;

import java.util.List;

public interface CdpWarehouseSemanticMetricFacade {

    List<SemanticMetricView> listMetrics(Long tenantId, String datasetKey);

    record SemanticMetricView(
            Long tenantId,
            String datasetKey,
            String metricKey,
            String expression,
            String valueType,
            List<String> allowedDimensions,
            String dimensionPolicy,
            String source) {
        public SemanticMetricView {
            allowedDimensions = allowedDimensions == null ? List.of() : List.copyOf(allowedDimensions);
        }
    }
}
