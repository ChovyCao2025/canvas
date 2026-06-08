package org.chovy.canvas.domain.bi.query;

import java.util.List;
import java.util.Map;

/**
 * MarketingBiDatasetRegistry 编排 domain.bi.query 场景的领域业务规则。
 */
public final class MarketingBiDatasetRegistry {

    private static final BiDatasetSpec CANVAS_DAILY_STATS = new BiDatasetSpec(
            "canvas_daily_stats",
            "canvas_dws.canvas_daily_stats",
            "tenant_id",
            Map.ofEntries(
                    Map.entry("stat_date", new BiFieldSpec("stat_date", "stat_date", BiFieldSpec.Role.DIMENSION, "DATE")),
                    Map.entry("canvas_id", new BiFieldSpec("canvas_id", "canvas_id", BiFieldSpec.Role.DIMENSION, "NUMBER")),
                    Map.entry("canvas_name", new BiFieldSpec("canvas_name", "canvas_name", BiFieldSpec.Role.DIMENSION, "STRING")),
                    Map.entry("trigger_type", new BiFieldSpec("trigger_type", "trigger_type", BiFieldSpec.Role.DIMENSION, "STRING")),
                    Map.entry("total_executions", new BiFieldSpec("total_executions", "total_executions", BiFieldSpec.Role.MEASURE, "NUMBER")),
                    Map.entry("success_count", new BiFieldSpec("success_count", "success_count", BiFieldSpec.Role.MEASURE, "NUMBER")),
                    Map.entry("fail_count", new BiFieldSpec("fail_count", "fail_count", BiFieldSpec.Role.MEASURE, "NUMBER")),
                    Map.entry("running_count", new BiFieldSpec("running_count", "running_count", BiFieldSpec.Role.MEASURE, "NUMBER")),
                    Map.entry("unique_users", new BiFieldSpec("unique_users", "unique_users", BiFieldSpec.Role.MEASURE, "NUMBER")),
                    Map.entry("avg_duration_ms", new BiFieldSpec("avg_duration_ms", "avg_duration_ms", BiFieldSpec.Role.MEASURE, "NUMBER"))
            ),
            Map.of(
                    /**
                     * 执行 BiMetricSpec 流程，围绕 bi metric spec 完成校验、计算或结果组装。
                     *
                     * @return 返回 BiMetricSpec 流程生成的业务结果。
                     */
                    "total_executions", new BiMetricSpec(
                            "total_executions", "SUM(total_executions)", "NUMBER", commonDimensions()),
                    /**
                     * 执行 BiMetricSpec 流程，围绕 bi metric spec 完成校验、计算或结果组装。
                     *
                     * @return 返回 BiMetricSpec 流程生成的业务结果。
                     */
                    "success_count", new BiMetricSpec("success_count", "SUM(success_count)", "NUMBER", commonDimensions()),
                    /**
                     * 执行 BiMetricSpec 流程，围绕 bi metric spec 完成校验、计算或结果组装。
                     *
                     * @return 返回 BiMetricSpec 流程生成的业务结果。
                     */
                    "fail_count", new BiMetricSpec("fail_count", "SUM(fail_count)", "NUMBER", commonDimensions()),
                    /**
                     * 执行 BiMetricSpec 流程，围绕 bi metric spec 完成校验、计算或结果组装。
                     *
                     * @return 返回 BiMetricSpec 流程生成的业务结果。
                     */
                    "unique_users", new BiMetricSpec("unique_users", "SUM(unique_users)", "NUMBER", commonDimensions()),
                    /**
                     * 执行 BiMetricSpec 流程，围绕 bi metric spec 完成校验、计算或结果组装。
                     *
                     * @return 返回 BiMetricSpec 流程生成的业务结果。
                     */
                    "avg_duration_ms", new BiMetricSpec(
                            "avg_duration_ms",
                            /**
                             * 执行 SUM 流程，围绕 sum 完成校验、计算或结果组装。
                             *
                             * @return 返回 SUM 流程生成的业务结果。
                             */
                            "CASE WHEN SUM(total_executions) > 0 THEN SUM(total_duration_ms) / SUM(total_executions) ELSE 0 END",
                            "NUMBER",
                            commonDimensions()),
                    /**
                     * 执行 BiMetricSpec 流程，围绕 bi metric spec 完成校验、计算或结果组装。
                     *
                     * @return 返回 BiMetricSpec 流程生成的业务结果。
                     */
                    "success_rate", new BiMetricSpec(
                            "success_rate",
                            /**
                             * 执行 SUM 流程，围绕 sum 完成校验、计算或结果组装。
                             *
                             * @return 返回 SUM 流程生成的业务结果。
                             */
                            "CASE WHEN SUM(total_executions) > 0 THEN SUM(success_count) / SUM(total_executions) ELSE 0 END",
                            "PERCENT",
                            commonDimensions())
            )
    );

    /**
     * 执行 MarketingBiDatasetRegistry 流程，围绕 marketing bi dataset registry 完成校验、计算或结果组装。
     */
    private MarketingBiDatasetRegistry() {
    }

    /**
     * 执行 commonDimensions 流程，围绕 common dimensions 完成校验、计算或结果组装。
     *
     * @return 返回 common dimensions 汇总后的集合、分页或映射视图。
     */
    private static List<String> commonDimensions() {
        return List.of("stat_date", "canvas_id", "canvas_name", "trigger_type");
    }

    /**
     * 返回平台内置营销 BI 数据集目录。
     *
     * <p>这些数据集用于看板预置、AI 取数规划和查询编译，字段、指标和允许维度共同定义可查询的数据口径；
     * 租户隔离仍由查询编译器通过数据集 tenantColumn 强制追加。</p>
     *
     * @return 不可变的内置数据集规格列表
     */
    public static List<BiDatasetSpec> datasets() {
        return List.of(CANVAS_DAILY_STATS);
    }

    /**
     * 按数据集 key 读取内置规格。
     *
     * @param datasetKey 数据集稳定业务 key
     * @return 匹配的数据集规格
     * @throws IllegalArgumentException 当 key 不属于内置营销 BI 数据集时抛出
     */
    public static BiDatasetSpec dataset(String datasetKey) {
        if (CANVAS_DAILY_STATS.datasetKey().equals(datasetKey)) {
            return CANVAS_DAILY_STATS;
        }
        throw new IllegalArgumentException("Unknown BI dataset: " + datasetKey);
    }
}
