package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.domain.bi.chart.BiChartResource;
import org.chovy.canvas.domain.bi.chart.BiChartResourceService;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardResource;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardResourceService;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardWidget;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiMetricSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
/**
 * CdpWarehouseMetricLineageService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehouseMetricLineageService {

    private static final String DEPENDENCY_EXPRESSION_FIELD = "EXPRESSION_FIELD";
    private static final String DEPENDENCY_ALLOWED_DIMENSION = "ALLOWED_DIMENSION";

    private final BiDatasetSpecResolver datasetSpecResolver;
    private final CdpWarehouseCatalogService catalogService;
    private final BiChartResourceService chartResourceService;
    private final BiDashboardResourceService dashboardResourceService;

    @Autowired
    /**
     * 初始化 CdpWarehouseMetricLineageService 实例。
     *
     * @param datasetSpecResolverProvider 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param catalogServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param chartResourceServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param dashboardResourceServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseMetricLineageService(
            ObjectProvider<BiDatasetSpecResolver> datasetSpecResolverProvider,
            ObjectProvider<CdpWarehouseCatalogService> catalogServiceProvider,
            ObjectProvider<BiChartResourceService> chartResourceServiceProvider,
            ObjectProvider<BiDashboardResourceService> dashboardResourceServiceProvider) {
        this(
                datasetSpecResolverProvider == null
                        ? BiDatasetSpecResolver.builtIn()
                        : datasetSpecResolverProvider.getIfAvailable(BiDatasetSpecResolver::builtIn),
                catalogServiceProvider == null ? null : catalogServiceProvider.getIfAvailable(),
                chartResourceServiceProvider == null ? null : chartResourceServiceProvider.getIfAvailable(),
                dashboardResourceServiceProvider == null ? null : dashboardResourceServiceProvider.getIfAvailable());
    }

    /**
     * 初始化 CdpWarehouseMetricLineageService 实例。
     *
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param catalogService 依赖组件，用于完成数据访问或外部能力调用。
     * @param chartResourceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param dashboardResourceService 依赖组件，用于完成数据访问或外部能力调用。
     */
    CdpWarehouseMetricLineageService(BiDatasetSpecResolver datasetSpecResolver,
                                     CdpWarehouseCatalogService catalogService,
                                     BiChartResourceService chartResourceService,
                                     BiDashboardResourceService dashboardResourceService) {
        this.datasetSpecResolver = datasetSpecResolver == null ? BiDatasetSpecResolver.builtIn() : datasetSpecResolver;
        this.catalogService = catalogService;
        this.chartResourceService = chartResourceService;
        this.dashboardResourceService = dashboardResourceService;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param metricKey 业务键，用于在同一租户下定位资源。
     * @return 返回 impact 流程生成的业务结果。
     */
    public MetricImpactView impact(Long tenantId, String datasetKey, String metricKey) {
        // 准备本次处理所需的上下文和中间变量。
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedDatasetKey = required(datasetKey, "datasetKey");
        String scopedMetricKey = required(metricKey, "metricKey");
        BiDatasetSpec dataset = datasetSpecResolver.dataset(scopedDatasetKey, scopedTenantId);
        BiMetricSpec metric = dataset.metrics().get(scopedMetricKey);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (metric == null) {
            throw new IllegalArgumentException("Unknown metric: " + scopedMetricKey);
        }

        List<String> warnings = new ArrayList<>();
        CdpWarehouseCatalogService.LineageGraph lineage = lineage(scopedTenantId, scopedDatasetKey, warnings);
        CdpWarehouseCatalogService.TransitiveLineageGraph transitiveLineage =
                transitiveLineage(scopedTenantId, scopedDatasetKey, warnings);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new MetricImpactView(
                scopedTenantId,
                scopedDatasetKey,
                scopedMetricKey,
                metric.expression(),
                metric.valueType(),
                metric.allowedDimensions(),
                fieldDependencies(dataset, metric),
                lineage == null ? List.of() : lineage.nodes(),
                lineage == null ? List.of() : lineage.edges(),
                transitiveLineage,
                impactedCharts(scopedTenantId, scopedDatasetKey, scopedMetricKey, warnings),
                impactedDashboards(scopedTenantId, scopedDatasetKey, scopedMetricKey, warnings),
                warnings);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param dataset dataset 参数，用于 fieldDependencies 流程中的校验、计算或对象转换。
     * @param metric metric 参数，用于 fieldDependencies 流程中的校验、计算或对象转换。
     * @return 返回 field dependencies 汇总后的集合、分页或映射视图。
     */
    private List<FieldDependencyView> fieldDependencies(BiDatasetSpec dataset, BiMetricSpec metric) {
        Map<String, FieldDependencyView> result = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String fieldKey : dataset.fields().keySet()) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (referencesField(metric.expression(), fieldKey)) {
                result.put(fieldKey + ":" + DEPENDENCY_EXPRESSION_FIELD,
                        new FieldDependencyView(fieldKey, DEPENDENCY_EXPRESSION_FIELD));
            }
        }
        for (String dimension : metric.allowedDimensions()) {
            result.put(dimension + ":" + DEPENDENCY_ALLOWED_DIMENSION,
                    new FieldDependencyView(dimension, DEPENDENCY_ALLOWED_DIMENSION));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return List.copyOf(result.values());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param warnings warnings 参数，用于 lineage 流程中的校验、计算或对象转换。
     * @return 返回 lineage 流程生成的业务结果。
     */
    private CdpWarehouseCatalogService.LineageGraph lineage(Long tenantId,
                                                            String datasetKey,
                                                            List<String> warnings) {
        if (catalogService == null) {
            addWarning(warnings, "warehouse catalog service is unavailable");
            return null;
        }
        try {
            return catalogService.lineage(tenantId, datasetKey, CdpWarehouseCatalogService.Direction.BOTH);
        } catch (RuntimeException e) {
            addWarning(warnings, "warehouse catalog lineage unavailable: " + e.getMessage());
            return null;
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param warnings warnings 参数，用于 transitiveLineage 流程中的校验、计算或对象转换。
     * @return 返回 transitiveLineage 流程生成的业务结果。
     */
    private CdpWarehouseCatalogService.TransitiveLineageGraph transitiveLineage(Long tenantId,
                                                                                String datasetKey,
                                                                                List<String> warnings) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (catalogService == null) {
            addWarning(warnings, "warehouse catalog service is unavailable");
            return null;
        }
        try {
            CdpWarehouseCatalogService.TransitiveLineageGraph graph =
                    catalogService.transitiveLineage(tenantId, datasetKey, CdpWarehouseCatalogService.Direction.BOTH, null);
            if (graph == null) {
                return null;
            }
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (String warning : graph.warnings()) {
                addWarning(warnings, warning);
            }
            return graph;
        } catch (RuntimeException e) {
            addWarning(warnings, "warehouse catalog transitive lineage unavailable: " + e.getMessage());
            // 汇总前面计算出的状态和明细，返回给调用方。
            return null;
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param metricKey 业务键，用于在同一租户下定位资源。
     * @param warnings warnings 参数，用于 impactedCharts 流程中的校验、计算或对象转换。
     * @return 返回 impacted charts 汇总后的集合、分页或映射视图。
     */
    private List<ChartImpactView> impactedCharts(Long tenantId,
                                                 String datasetKey,
                                                 String metricKey,
                                                 List<String> warnings) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (chartResourceService == null) {
            warnings.add("BI chart resource service is unavailable");
            return List.of();
        }
        try {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            return chartResourceService.list(tenantId).stream()
                    .filter(chart -> chart != null
                            && datasetKey.equals(chart.datasetKey())
                            && chart.query() != null
                            && chart.query().metrics().contains(metricKey))
                    .map(chart -> new ChartImpactView(
                            chart.chartKey(),
                            chart.name(),
                            chart.chartType(),
                            chart.status(),
                            chart.query().dimensions()))
                    .toList();
        } catch (RuntimeException e) {
            warnings.add("BI chart impact unavailable: " + e.getMessage());
            // 汇总前面计算出的状态和明细，返回给调用方。
            return List.of();
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param metricKey 业务键，用于在同一租户下定位资源。
     * @param warnings warnings 参数，用于 impactedDashboards 流程中的校验、计算或对象转换。
     * @return 返回 impacted dashboards 汇总后的集合、分页或映射视图。
     */
    private List<DashboardImpactView> impactedDashboards(Long tenantId,
                                                         String datasetKey,
                                                         String metricKey,
                                                         List<String> warnings) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (dashboardResourceService == null) {
            warnings.add("BI dashboard resource service is unavailable");
            return List.of();
        }
        try {
            List<DashboardImpactView> result = new ArrayList<>();
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (BiDashboardResource dashboard : dashboardResourceService.list(tenantId)) {
                if (dashboard == null || dashboard.preset() == null
                        || !datasetKey.equals(dashboard.preset().datasetKey())) {
                    continue;
                }
                List<String> widgetKeys = dashboard.preset().widgets().stream()
                        .filter(widget -> widgetUsesMetric(widget, metricKey))
                        .map(BiDashboardWidget::widgetKey)
                        .toList();
                if (!widgetKeys.isEmpty()) {
                    result.add(new DashboardImpactView(
                            dashboard.preset().dashboardKey(),
                            dashboard.preset().title(),
                            dashboard.status(),
                            dashboard.version(),
                            widgetKeys));
                }
            }
            return result;
        } catch (RuntimeException e) {
            warnings.add("BI dashboard impact unavailable: " + e.getMessage());
            // 汇总前面计算出的状态和明细，返回给调用方。
            return List.of();
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param widget widget 参数，用于 widgetUsesMetric 流程中的校验、计算或对象转换。
     * @param metricKey 业务键，用于在同一租户下定位资源。
     * @return 返回 widget uses metric 的布尔判断结果。
     */
    private boolean widgetUsesMetric(BiDashboardWidget widget, String metricKey) {
        return widget != null && widget.metrics() != null && widget.metrics().contains(metricKey);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param expression expression 参数，用于 referencesField 流程中的校验、计算或对象转换。
     * @param fieldKey 业务键，用于在同一租户下定位资源。
     * @return 返回 references field 的布尔判断结果。
     */
    private boolean referencesField(String expression, String fieldKey) {
        if (!hasText(expression) || !hasText(fieldKey)) {
            return false;
        }
        return Pattern.compile("(?i)(?<![A-Za-z0-9_])" + Pattern.quote(fieldKey) + "(?![A-Za-z0-9_])")
                .matcher(expression)
                .find();
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
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
     * 创建业务对象并完成必要的初始化。
     *
     * @param warnings warnings 参数，用于 addWarning 流程中的校验、计算或对象转换。
     * @param warning warning 参数，用于 addWarning 流程中的校验、计算或对象转换。
     */
    private void addWarning(List<String> warnings, String warning) {
        if (hasText(warning) && !warnings.contains(warning)) {
            warnings.add(warning);
        }
    }

    /**
     * MetricImpactView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record MetricImpactView(
            Long tenantId,
            String datasetKey,
            String metricKey,
            String expression,
            String valueType,
            List<String> allowedDimensions,
            List<FieldDependencyView> fieldDependencies,
            List<CdpWarehouseCatalogService.DatasetView> lineageNodes,
            List<CdpWarehouseCatalogService.LineageEdgeView> lineageEdges,
            CdpWarehouseCatalogService.TransitiveLineageGraph transitiveLineage,
            List<ChartImpactView> charts,
            List<DashboardImpactView> dashboards,
            List<String> warnings) {
        /**
         * 初始化 MetricImpactView 实例。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param datasetKey 业务键，用于在同一租户下定位资源。
         * @param metricKey 业务键，用于在同一租户下定位资源。
         * @param expression expression 参数，用于 MetricImpactView 流程中的校验、计算或对象转换。
         * @param valueType 类型标识，用于选择对应处理分支。
         * @param allowedDimensions allowed dimensions 参数，用于 MetricImpactView 流程中的校验、计算或对象转换。
         * @param fieldDependencies field dependencies 参数，用于 MetricImpactView 流程中的校验、计算或对象转换。
         * @param lineageNodes lineage nodes 参数，用于 MetricImpactView 流程中的校验、计算或对象转换。
         * @param lineageEdges lineage edges 参数，用于 MetricImpactView 流程中的校验、计算或对象转换。
         * @param charts charts 参数，用于 MetricImpactView 流程中的校验、计算或对象转换。
         * @param dashboards dashboards 参数，用于 MetricImpactView 流程中的校验、计算或对象转换。
         * @param warnings warnings 参数，用于 MetricImpactView 流程中的校验、计算或对象转换。
         */
        public MetricImpactView(Long tenantId,
                                String datasetKey,
                                String metricKey,
                                String expression,
                                String valueType,
                                List<String> allowedDimensions,
                                List<FieldDependencyView> fieldDependencies,
                                List<CdpWarehouseCatalogService.DatasetView> lineageNodes,
                                List<CdpWarehouseCatalogService.LineageEdgeView> lineageEdges,
                                List<ChartImpactView> charts,
                                List<DashboardImpactView> dashboards,
                                List<String> warnings) {
            this(tenantId, datasetKey, metricKey, expression, valueType, allowedDimensions,
                    fieldDependencies, lineageNodes, lineageEdges, null, charts, dashboards, warnings);
        }

        public MetricImpactView {
            allowedDimensions = allowedDimensions == null ? List.of() : List.copyOf(allowedDimensions);
            fieldDependencies = fieldDependencies == null ? List.of() : List.copyOf(fieldDependencies);
            lineageNodes = lineageNodes == null ? List.of() : List.copyOf(lineageNodes);
            lineageEdges = lineageEdges == null ? List.of() : List.copyOf(lineageEdges);
            charts = charts == null ? List.of() : List.copyOf(charts);
            dashboards = dashboards == null ? List.of() : List.copyOf(dashboards);
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }
    }

    /**
     * FieldDependencyView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record FieldDependencyView(String fieldKey, String dependencyType) {
    }

    /**
     * ChartImpactView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ChartImpactView(
            String chartKey,
            String name,
            String chartType,
            String status,
            List<String> dimensions) {
        public ChartImpactView {
            dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
        }
    }

    /**
     * DashboardImpactView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record DashboardImpactView(
            String dashboardKey,
            String title,
            String status,
            int version,
            List<String> widgetKeys) {
        public DashboardImpactView {
            widgetKeys = widgetKeys == null ? List.of() : List.copyOf(widgetKeys);
        }
    }
}
