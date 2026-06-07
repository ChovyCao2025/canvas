package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiMetricSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CdpWarehouseSemanticMetricService {

    private final BiDatasetSpecResolver datasetSpecResolver;

    @Autowired
    public CdpWarehouseSemanticMetricService(ObjectProvider<BiDatasetSpecResolver> datasetSpecResolverProvider) {
        this(datasetSpecResolverProvider == null
                ? BiDatasetSpecResolver.builtIn()
                : datasetSpecResolverProvider.getIfAvailable(BiDatasetSpecResolver::builtIn));
    }

    CdpWarehouseSemanticMetricService(BiDatasetSpecResolver datasetSpecResolver) {
        this.datasetSpecResolver = datasetSpecResolver == null ? BiDatasetSpecResolver.builtIn() : datasetSpecResolver;
    }

    public List<SemanticMetricView> listMetrics(Long tenantId, String datasetKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        if (hasText(datasetKey)) {
            return metrics(scopedTenantId, datasetSpecResolver.dataset(datasetKey.trim(), scopedTenantId));
        }
        List<SemanticMetricView> result = new ArrayList<>();
        for (BiDatasetSpec dataset : datasetSpecResolver.datasets(scopedTenantId)) {
            result.addAll(metrics(scopedTenantId, dataset));
        }
        return List.copyOf(result);
    }

    private List<SemanticMetricView> metrics(Long tenantId, BiDatasetSpec dataset) {
        if (dataset == null || dataset.metrics() == null) {
            return List.of();
        }
        return dataset.metrics().values().stream()
                .map(metric -> toView(tenantId, dataset, metric))
                .toList();
    }

    private SemanticMetricView toView(Long tenantId, BiDatasetSpec dataset, BiMetricSpec metric) {
        return new SemanticMetricView(
                tenantId,
                dataset.datasetKey(),
                metric.metricKey(),
                metric.expression(),
                metric.valueType(),
                metric.allowedDimensions(),
                metric.allowedDimensions().isEmpty() ? "ALL_DATASET_DIMENSIONS" : "ALLOW_LIST",
                "BI_DATASET_SPEC");
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record SemanticMetricView(
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
