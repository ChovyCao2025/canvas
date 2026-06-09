package org.chovy.canvas.domain.warehouse;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CdpWarehouseDorisPrometheusMetricsParser 编排 domain.warehouse 场景的领域业务规则。
 */
public class CdpWarehouseDorisPrometheusMetricsParser {

    /**
     * parse 校验或转换 domain.warehouse 场景的数据。
     * @param endpoint endpoint 参数，用于 parse 流程中的校验、计算或对象转换。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param body 待处理业务值，用于规则计算、转换或外部调用。
     * @param measuredAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回解析、归一化或安全处理后的值。
     */
    public ParsedMetrics parse(String endpoint, String role, String body, LocalDateTime measuredAt) {
        Map<String, Double> values = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String line : safeBody(body).split("\\R")) {
            String trimmed = line.trim();
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            MetricLine metric = parseLine(trimmed);
            if (metric == null) {
                continue;
            }
            values.merge(metric.name(), metric.value(), Math::max);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ParsedMetrics(
                defaultString(endpoint, "unknown"),
                defaultString(role, "UNKNOWN").toUpperCase(Locale.ROOT),
                measuredAt == null ? LocalDateTime.now() : measuredAt,
                values);
    }

    /**
     * 解析并校验输入数据。
     *
     * @param line line 参数，用于 parseLine 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private MetricLine parseLine(String line) {
        int valueStart = line.lastIndexOf(' ');
        if (valueStart <= 0 || valueStart >= line.length() - 1) {
            return null;
        }
        String metricPart = line.substring(0, valueStart).trim();
        String valuePart = line.substring(valueStart + 1).trim();
        String name = metricPart;
        int labelStart = metricPart.indexOf('{');
        if (labelStart >= 0) {
            name = metricPart.substring(0, labelStart);
        }
        if (name.isBlank()) {
            return null;
        }
        try {
            return new MetricLine(name, Double.parseDouble(valuePart));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param body 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 safe body 生成的文本或业务键。
     */
    private String safeBody(String body) {
        return body == null ? "" : body;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /**
     * MetricLine 数据记录。
     */
    private record MetricLine(String name, double value) {
    }

    /**
     * ParsedMetrics 校验或转换 domain.warehouse 场景的数据。
     * @param endpoint endpoint 参数，用于 ParsedMetrics 流程中的校验、计算或对象转换。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param measuredAt 时间参数，用于计算窗口、过期或审计时间。
     * @param metrics metrics 参数，用于 ParsedMetrics 流程中的校验、计算或对象转换。
     * @return 返回 ParsedMetrics 流程生成的业务结果。
     */
    public record ParsedMetrics(
            String endpoint,
            String role,
            LocalDateTime measuredAt,
            Map<String, Double> metrics) {
        public ParsedMetrics {
            metrics = metrics == null ? Map.of() : Map.copyOf(metrics);
        }

        /**
         * hasMetric 校验或转换 domain.warehouse 场景的数据。
         * @param name 名称文本，用于展示或唯一性校验。
         * @return 返回布尔判断结果。
         */
        public boolean hasMetric(String name) {
            return metrics.containsKey(name);
        }

        /**
         * max 处理 domain.warehouse 场景的业务逻辑。
         * @param name 名称文本，用于展示或唯一性校验。
         * @return 返回 max 计算得到的数量、金额或指标值。
         */
        public double max(String name) {
            return metrics.getOrDefault(name, Double.NaN);
        }

        /**
         * metricNames 处理 domain.warehouse 场景的业务逻辑。
         * @return 返回 metric names 汇总后的集合、分页或映射视图。
         */
        public List<String> metricNames() {
            return metrics.keySet().stream().sorted().toList();
        }
    }
}
