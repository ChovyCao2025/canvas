package org.chovy.canvas.domain.warehouse;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CdpWarehouseDorisPrometheusMetricsParser {

    public ParsedMetrics parse(String endpoint, String role, String body, LocalDateTime measuredAt) {
        Map<String, Double> values = new LinkedHashMap<>();
        for (String line : safeBody(body).split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            MetricLine metric = parseLine(trimmed);
            if (metric == null) {
                continue;
            }
            values.merge(metric.name(), metric.value(), Math::max);
        }
        return new ParsedMetrics(
                defaultString(endpoint, "unknown"),
                defaultString(role, "UNKNOWN").toUpperCase(Locale.ROOT),
                measuredAt == null ? LocalDateTime.now() : measuredAt,
                values);
    }

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
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String safeBody(String body) {
        return body == null ? "" : body;
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private record MetricLine(String name, double value) {
    }

    public record ParsedMetrics(
            String endpoint,
            String role,
            LocalDateTime measuredAt,
            Map<String, Double> metrics) {
        public ParsedMetrics {
            metrics = metrics == null ? Map.of() : Map.copyOf(metrics);
        }

        public boolean hasMetric(String name) {
            return metrics.containsKey(name);
        }

        public double max(String name) {
            return metrics.getOrDefault(name, Double.NaN);
        }

        public List<String> metricNames() {
            return metrics.keySet().stream().sorted().toList();
        }
    }
}
