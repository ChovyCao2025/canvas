package org.chovy.canvas.bi.domain;

import org.chovy.canvas.bi.api.BiAiRequestCommand;
import org.chovy.canvas.bi.api.BiAiResponseView;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
/**
 * BiAiAssistantCatalog 目录服务。
 */
public class BiAiAssistantCatalog {
    /**
     * 执行 answer 相关处理。
     */
    public BiAiResponseView answer(Long tenantId, String operation, BiAiRequestCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("BI AI request is required");
        }
        Long scopedTenantId = tenant(tenantId);
        String normalizedOperation = operation(operation);
        String normalizedActor = actor(actor);
        String datasetKey = optionalKey(command.datasetKey());
        String resourceType = optionalType(command.resourceType());
        String resourceKey = optionalKey(command.resourceKey());
        String question = firstText(command.question(), command.prompt(), command.title(), normalizedOperation);
        Map<String, Object> metadata = metadata(command, datasetKey, resourceType, resourceKey);
        String runId = "bi-ai-" + scopedTenantId + "-" + normalizedOperation.toLowerCase(Locale.ROOT);
        String explanation = explanation(normalizedOperation, question, datasetKey, resourceType, resourceKey);
        String summary = summary(normalizedOperation, datasetKey, resourceType, resourceKey);
        List<Map<String, Object>> sections = command.sections() == null ? List.of() : List.copyOf(command.sections());
        Map<String, Object> dashboard = dashboard(command, datasetKey);

        return new BiAiResponseView(
                scopedTenantId,
                normalizedActor,
                normalizedOperation,
                runId,
                question,
                "READY",
                true,
                explanation,
                metadata,
                summary,
                keyFindings(resourceType, resourceKey),
                recommendations(normalizedOperation),
                title(command, normalizedOperation),
                sections,
                nextActions(normalizedOperation),
                dashboard,
                charts(dashboard),
                trends(normalizedOperation),
                anomalies(normalizedOperation),
                opportunities(normalizedOperation));
    }
    /**
     * 执行 metadata 相关处理。
     */
    private static Map<String, Object> metadata(BiAiRequestCommand command,
                                                String datasetKey,
                                                String resourceType,
                                                String resourceKey) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        putIfPresent(metadata, "datasetKey", datasetKey);
        putIfPresent(metadata, "resourceType", resourceType);
        putIfPresent(metadata, "resourceKey", resourceKey);
        putIfPresent(metadata, "reportType", optionalKey(command.reportType()));
        putIfPresent(metadata, "modelKey", text(command.modelKey()));
        putIfPresent(metadata, "limit", command.limit());
        putIfPresent(metadata, "providerId", command.providerId());
        putIfPresent(metadata, "templateId", command.templateId());
        putIfPresent(metadata, "timeoutMs", command.timeoutMs());
        metadata.put("params", command.params() == null ? Map.of() : Map.copyOf(command.params()));
        metadata.put("subject", command.subject() == null ? Map.of() : Map.copyOf(command.subject()));
        metadata.put("result", command.result() == null ? Map.of() : Map.copyOf(command.result()));
        metadata.put("metrics", command.metrics() == null ? Map.of() : Map.copyOf(command.metrics()));
        metadata.put("context", command.context() == null ? Map.of() : Map.copyOf(command.context()));
        return metadata;
    }
    /**
     * 执行 explanation 相关处理。
     */
    private static String explanation(String operation,
                                      String question,
                                      String datasetKey,
                                      String resourceType,
                                      String resourceKey) {
        String subject = subject(datasetKey, resourceType, resourceKey);
        return "Compact BI AI " + operation + " response for " + subject + ": " + question;
    }
    /**
     * 执行 summary 相关处理。
     */
    private static String summary(String operation, String datasetKey, String resourceType, String resourceKey) {
        return "Compact " + operation + " summary for " + subject(datasetKey, resourceType, resourceKey);
    }
    /**
     * 执行 subject 相关处理。
     */
    private static String subject(String datasetKey, String resourceType, String resourceKey) {
        if (resourceType != null && resourceKey != null) {
            return resourceType + "/" + resourceKey;
        }
        return datasetKey == null ? "general-bi-context" : datasetKey;
    }
    /**
     * 执行 key Findings 相关处理。
     */
    private static List<String> keyFindings(String resourceType, String resourceKey) {
        if (resourceType != null && resourceKey != null) {
            return List.of("Subject " + resourceType + "/" + resourceKey + " is ready for BI review");
        }
        return List.of("Current BI request is ready for deterministic compact review");
    }
    /**
     * 执行 recommendations 相关处理。
     */
    private static List<String> recommendations(String operation) {
        if ("INTERPRET".equals(operation)) {
            return List.of("Validate the generated interpretation against source dashboards");
        }
        return List.of("Review deterministic BI AI output before publishing");
    }
    /**
     * 执行 title 相关处理。
     */
    private static String title(BiAiRequestCommand command, String operation) {
        String title = text(command.title());
        if (title != null) {
            return title;
        }
        return "REPORT".equals(operation) ? "BI AI Report" : "BI AI " + operation;
    }
    /**
     * 执行 next Actions 相关处理。
     */
    private static List<String> nextActions(String operation) {
        if ("REPORT".equals(operation)) {
            return List.of("Review report narrative", "Attach approved dashboard evidence");
        }
        return List.of("Review generated BI AI output");
    }
    /**
     * 执行 dashboard 相关处理。
     */
    private static Map<String, Object> dashboard(BiAiRequestCommand command, String datasetKey) {
        String key = datasetKey == null ? optionalKey(command.prompt()) : datasetKey;
        if (key == null) {
            key = "general";
        }
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("dashboardKey", "ai-draft-" + key);
        dashboard.put("title", firstText(command.title(), command.prompt(), "AI Draft Dashboard"));
        dashboard.put("datasetKey", datasetKey);
        dashboard.put("layout", command.params() == null ? Map.of() : Map.copyOf(command.params()));
        return dashboard;
    }
    /**
     * 执行 charts 相关处理。
     */
    private static List<Map<String, Object>> charts(Map<String, Object> dashboard) {
        Object dashboardKey = dashboard.get("dashboardKey");
        return List.of(Map.of(
                "chartKey", dashboardKey + "-trend",
                "chartType", "LINE",
                "title", "AI trend"));
    }
    /**
     * 执行 trends 相关处理。
     */
    private static List<String> trends(String operation) {
        return "INSIGHTS".equals(operation) ? List.of("Current result is prepared for trend review") : List.of();
    }
    /**
     * 执行 anomalies 相关处理。
     */
    private static List<String> anomalies(String operation) {
        return "INSIGHTS".equals(operation)
                ? List.of("No deterministic anomaly detected in compact mode")
                : List.of();
    }
    /**
     * 执行 opportunities 相关处理。
     */
    private static List<String> opportunities(String operation) {
        return "INSIGHTS".equals(operation)
                ? List.of("Use the insight as a draft before publishing BI decisions")
                : List.of();
    }
    /**
     * 执行 operation 相关处理。
     */
    private static String operation(String operation) {
        String normalized = text(operation);
        if (normalized == null) {
            throw new IllegalArgumentException("BI AI operation is required");
        }
        normalized = normalized.toUpperCase(Locale.ROOT)
                .replace("ASK_DATA", "ASK")
                .replace("ASK-DATA", "ASK")
                .replace('-', '_');
        return switch (normalized) {
            case "ASK", "INTERPRET", "REPORT", "DASHBOARD_DRAFT", "INSIGHTS" -> normalized;
            default -> throw new IllegalArgumentException("Unsupported BI AI operation: " + operation);
        };
    }
    /**
     * 执行 optional Type 相关处理。
     */
    private static String optionalType(String value) {
        String text = text(value);
        if (text == null) {
            return null;
        }
        return text.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
    }
    /**
     * 执行 optional Key 相关处理。
     */
    private static String optionalKey(String value) {
        String text = text(value);
        if (text == null) {
            return null;
        }
        return BiResourceKey.of(text, "key").value();
    }
    /**
     * 执行 first Text 相关处理。
     */
    private static String firstText(String... values) {
        for (String value : values) {
            String text = text(value);
            if (text != null) {
                return text;
            }
        }
        return "BI AI request";
    }
    /**
     * 执行 text 相关处理。
     */
    private static String text(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
    /**
     * 执行 tenant 相关处理。
     */
    private static Long tenant(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }
    /**
     * 执行 actor 相关处理。
     */
    private static String actor(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }
    /**
     * 执行 put If Present 相关处理。
     */
    private static void putIfPresent(Map<String, Object> metadata, String key, Object value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }
}
