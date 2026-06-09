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
/**
 * CdpWarehouseSemanticMetricService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehouseSemanticMetricService {

    private final BiDatasetSpecResolver datasetSpecResolver;

    @Autowired
    /**
     * 初始化 CdpWarehouseSemanticMetricService 实例。
     *
     * @param datasetSpecResolverProvider 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public CdpWarehouseSemanticMetricService(ObjectProvider<BiDatasetSpecResolver> datasetSpecResolverProvider) {
        this(datasetSpecResolverProvider == null
                ? BiDatasetSpecResolver.builtIn()
                : datasetSpecResolverProvider.getIfAvailable(BiDatasetSpecResolver::builtIn));
    }

    /**
     * 初始化 CdpWarehouseSemanticMetricService 实例。
     *
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    CdpWarehouseSemanticMetricService(BiDatasetSpecResolver datasetSpecResolver) {
        this.datasetSpecResolver = datasetSpecResolver == null ? BiDatasetSpecResolver.builtIn() : datasetSpecResolver;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param dataset dataset 参数，用于 metrics 流程中的校验、计算或对象转换。
     * @return 返回 metrics 汇总后的集合、分页或映射视图。
     */
    private List<SemanticMetricView> metrics(Long tenantId, BiDatasetSpec dataset) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (dataset == null || dataset.metrics() == null) {
            return List.of();
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return dataset.metrics().values().stream()
                .map(metric -> toView(tenantId, dataset, metric))
                .toList();
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param dataset dataset 参数，用于 toView 流程中的校验、计算或对象转换。
     * @param metric metric 参数，用于 toView 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * SemanticMetricView 承载对应领域的业务规则、流程编排和结果转换。
     */
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
