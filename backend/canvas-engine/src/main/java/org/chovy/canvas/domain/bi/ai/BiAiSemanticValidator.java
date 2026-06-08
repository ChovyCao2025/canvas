package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.chart.BiChartResource;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardPreset;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardWidget;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiQueryCompiler;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;

import java.util.List;

/**
 * BiAiSemanticValidator 业务组件。
 */
final class BiAiSemanticValidator {

    private final BiDatasetSpecResolver datasetSpecResolver;
    private final BiQueryCompiler compiler = new BiQueryCompiler();

    /**
     * 执行 BiAiSemanticValidator 流程，围绕 bi ai semantic validator 完成校验、计算或结果组装。
     *
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    BiAiSemanticValidator(BiDatasetSpecResolver datasetSpecResolver) {
        this.datasetSpecResolver = datasetSpecResolver == null ? BiDatasetSpecResolver.builtIn() : datasetSpecResolver;
    }

    /**
     * 执行 catalog 流程，围绕 catalog 完成校验、计算或结果组装。
     *
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 catalog 汇总后的集合、分页或映射视图。
     */
    List<BiDatasetSpec> catalog(String datasetKey, Long tenantId) {
        String scopedDatasetKey = trimToNull(datasetKey);
        if (scopedDatasetKey == null) {
            return datasetSpecResolver.datasets(tenantId);
        }
        return List.of(datasetSpecResolver.dataset(scopedDatasetKey, tenantId));
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param query query 参数，用于 validateQuery 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回布尔判断结果。
     */
    BiDatasetSpec validateQuery(BiQueryRequest query, Long tenantId) {
        if (query == null) {
            throw new IllegalArgumentException("query is required");
        }
        BiDatasetSpec dataset = datasetSpecResolver.dataset(query.datasetKey(), tenantId);
        compiler.compile(dataset, query, tenantId);
        return dataset;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param result result 参数，用于 validateResultDataset 流程中的校验、计算或对象转换。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     */
    void validateResultDataset(BiQueryResult result, String datasetKey, String fieldName) {
        if (result == null) {
            return;
        }
        if (!result.datasetKey().equals(datasetKey)) {
            throw new IllegalArgumentException(fieldName + " dataset does not match query dataset");
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param plan plan 参数，用于 validateDashboardDraft 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     */
    void validateDashboardDraft(BiDashboardDraftPlan plan, Long tenantId) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (plan == null || plan.dashboard() == null) {
            throw new IllegalArgumentException("dashboard draft is required");
        }
        BiDashboardPreset dashboard = plan.dashboard();
        String datasetKey = required(dashboard.datasetKey(), "dashboard datasetKey");
        datasetSpecResolver.dataset(datasetKey, tenantId);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (BiDashboardWidget widget : dashboard.widgets()) {
            validateWidget(datasetKey, widget, tenantId);
        }
        for (BiChartResource chart : plan.charts()) {
            if (!datasetKey.equals(chart.datasetKey())) {
                throw new IllegalArgumentException("chart dataset does not match dashboard dataset");
            }
            validateQuery(chart.query(), tenantId);
        }
    }

    /**
     * 校验 LLM 草稿中的看板组件是否能被 BI 查询编译器接受。
     *
     * <p>组件只携带维度和指标，方法会临时构造最小查询请求并交给编译器验证字段白名单、指标维度约束和租户列口径；
     * 这样可以在草稿落库前拦截模型幻觉字段或跨数据集指标。</p>
     */
    private void validateWidget(String datasetKey, BiDashboardWidget widget, Long tenantId) {
        if (widget == null) {
            throw new IllegalArgumentException("dashboard widget is required");
        }
        validateQuery(new BiQueryRequest(
                datasetKey,
                widget.dimensions(),
                widget.metrics(),
                List.of(),
                List.of(),
                100), tenantId);
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return trimmed;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
