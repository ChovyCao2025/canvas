package org.chovy.canvas.platform.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AiCatalog {

    private final Map<Long, List<Map<String, Object>>> decisionRunsByTenant = new LinkedHashMap<>();
    private final Map<Long, List<Map<String, Object>>> predictionRunsByTenant = new LinkedHashMap<>();
    private final Map<Long, Map<Long, Map<String, Object>>> promptTemplatesByTenant = new LinkedHashMap<>();
    private final Map<Long, List<Map<String, Object>>> evaluationAuditsByTenant = new LinkedHashMap<>();
    private final Map<Long, Map<Long, Map<String, Object>>> providersByTenant = new LinkedHashMap<>();

    public Map<String, Object> recomputeDecision(Long tenantId, Map<String, Object> payload, String actor) {
        List<Map<String, Object>> runs = decisionRunsByTenant.computeIfAbsent(tenantId, ignored -> new ArrayList<>());
        Map<String, Object> run = new LinkedHashMap<>();
        run.put("runId", "ai-decision-" + tenantId + "-" + (runs.size() + 1));
        run.put("tenantId", tenantId);
        run.put("decisionScope", stringValue(payload.get("decisionScope"), "DEFAULT"));
        run.put("status", "COMPLETED");
        run.put("triggeredBy", actor);
        runs.add(run);
        return copy(run);
    }

    public Map<String, Object> latestDecisionRun(Long tenantId, String decisionScope) {
        return latest(decisionRunsByTenant.getOrDefault(tenantId, List.of()), decisionScope, "decisionScope",
                "decision run not found");
    }

    public List<Map<String, Object>> decisionRecommendations(Long tenantId, Long runId, String decisionType,
                                                             String eligibilityStatus, int limit) {
        List<Map<String, Object>> recommendations = List.of(
                recommendation(tenantId, 7001L, "COUPON", "ELIGIBLE", 0.91),
                recommendation(tenantId, 7002L, "MESSAGE", "ELIGIBLE", 0.87));
        return recommendations.stream()
                .filter(item -> matches(item, "decisionType", decisionType))
                .filter(item -> matches(item, "eligibilityStatus", eligibilityStatus))
                .limit(limit)
                .map(AiCatalog::copy)
                .toList();
    }

    public Map<String, Object> recordDecisionFeedback(Long tenantId, Long recommendationId,
                                                      Map<String, Object> payload, String actor) {
        Map<String, Object> feedback = new LinkedHashMap<>(payload);
        feedback.put("tenantId", tenantId);
        feedback.put("recommendationId", recommendationId);
        feedback.put("feedbackStatus", "RECORDED");
        feedback.put("updatedBy", actor);
        return feedback;
    }

    public Map<String, Object> latestPredictionRun(Long tenantId) {
        List<Map<String, Object>> runs = predictionRunsByTenant.getOrDefault(tenantId, List.of());
        if (runs.isEmpty()) {
            return Map.of("tenantId", tenantId, "status", "NOT_STARTED");
        }
        return copy(runs.get(runs.size() - 1));
    }

    public Map<String, Object> predictionReadiness(Long tenantId) {
        return Map.of("tenantId", tenantId, "ready", true, "requiredSignals", 3, "availableSignals", 3);
    }

    public List<Map<String, Object>> churnDistribution(Long tenantId) {
        return List.of(
                Map.of("tenantId", tenantId, "bucket", "LOW", "users", 120),
                Map.of("tenantId", tenantId, "bucket", "MEDIUM", "users", 48),
                Map.of("tenantId", tenantId, "bucket", "HIGH", "users", 17));
    }

    public List<Map<String, Object>> topRiskUsers(Long tenantId, int limit) {
        return List.of(
                riskUser(tenantId, 9001L, 0.94),
                riskUser(tenantId, 9002L, 0.89),
                riskUser(tenantId, 9003L, 0.83)).stream()
                .limit(limit)
                .map(AiCatalog::copy)
                .toList();
    }

    public Map<String, Object> recomputePrediction(Long tenantId, Map<String, Object> payload) {
        List<Map<String, Object>> runs = predictionRunsByTenant.computeIfAbsent(tenantId, ignored -> new ArrayList<>());
        Map<String, Object> run = new LinkedHashMap<>(payload);
        run.put("runId", "ai-prediction-" + tenantId + "-" + (runs.size() + 1));
        run.put("tenantId", tenantId);
        run.put("status", "COMPLETED");
        runs.add(run);
        return copy(run);
    }

    public List<Map<String, Object>> promptTemplates(Long tenantId) {
        return promptTemplatesByTenant.getOrDefault(tenantId, Map.of()).values().stream()
                .map(AiCatalog::copy)
                .toList();
    }

    public Map<String, Object> createPromptTemplate(Long tenantId, Map<String, Object> payload, String actor) {
        String name = requiredString(payload, "name");
        Long id = nextId(promptTemplatesByTenant.computeIfAbsent(tenantId, ignored -> new LinkedHashMap<>()));
        Map<String, Object> template = new LinkedHashMap<>(payload);
        template.put("id", id);
        template.put("tenantId", tenantId);
        template.put("name", name);
        template.put("status", "ENABLED");
        template.put("createdBy", actor);
        promptTemplatesByTenant.get(tenantId).put(id, template);
        return copy(template);
    }

    public Map<String, Object> promptTemplate(Long tenantId, Long id) {
        return copy(find(promptTemplatesByTenant, tenantId, id, "prompt template not found"));
    }

    public Map<String, Object> updatePromptTemplate(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        Map<String, Object> template = find(promptTemplatesByTenant, tenantId, id, "prompt template not found");
        template.putAll(payload);
        template.put("updatedBy", actor);
        return copy(template);
    }

    public Map<String, Object> disablePromptTemplate(Long tenantId, Long id, String actor) {
        Map<String, Object> template = find(promptTemplatesByTenant, tenantId, id, "prompt template not found");
        template.put("status", "DISABLED");
        template.put("updatedBy", actor);
        return copy(template);
    }

    public Map<String, Object> renderPromptTemplate(Long tenantId, Map<String, Object> payload) {
        Long templateId = longValue(payload.get("templateId"));
        Map<String, Object> template = promptTemplate(tenantId, templateId);
        String rendered = String.valueOf(template.getOrDefault("template", ""));
        Object variables = payload.get("variables");
        if (variables instanceof Map<?, ?> values) {
            for (Map.Entry<?, ?> entry : values.entrySet()) {
                rendered = rendered.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
            }
        }
        return Map.of("tenantId", tenantId, "templateId", templateId, "renderedPrompt", rendered);
    }

    public Map<String, Object> evaluatePromptTemplate(Long tenantId, Map<String, Object> payload) {
        Long templateId = longValue(payload.get("templateId"));
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("tenantId", tenantId);
        audit.put("templateId", templateId);
        audit.put("status", "PASSED");
        audit.put("sampleInput", payload.get("sampleInput"));
        evaluationAuditsByTenant.computeIfAbsent(tenantId, ignored -> new ArrayList<>()).add(audit);
        return copy(audit);
    }

    public List<Map<String, Object>> evaluationAudits(Long tenantId) {
        return evaluationAuditsByTenant.getOrDefault(tenantId, List.of()).stream()
                .map(AiCatalog::copy)
                .toList();
    }

    public List<Map<String, Object>> providers(Long tenantId) {
        return providersByTenant.getOrDefault(tenantId, Map.of()).values().stream()
                .map(AiCatalog::copy)
                .toList();
    }

    public Map<String, Object> createProvider(Long tenantId, Map<String, Object> payload, String actor) {
        requiredString(payload, "name");
        Long id = nextId(providersByTenant.computeIfAbsent(tenantId, ignored -> new LinkedHashMap<>()));
        Map<String, Object> provider = new LinkedHashMap<>(payload);
        provider.put("id", id);
        provider.put("tenantId", tenantId);
        provider.put("status", "ENABLED");
        provider.put("createdBy", actor);
        providersByTenant.get(tenantId).put(id, provider);
        return copy(provider);
    }

    public Map<String, Object> provider(Long tenantId, Long id) {
        return copy(find(providersByTenant, tenantId, id, "provider not found"));
    }

    public Map<String, Object> updateProvider(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        Map<String, Object> provider = find(providersByTenant, tenantId, id, "provider not found");
        provider.putAll(payload);
        provider.put("updatedBy", actor);
        return copy(provider);
    }

    public Map<String, Object> disableProvider(Long tenantId, Long id, String actor) {
        Map<String, Object> provider = find(providersByTenant, tenantId, id, "provider not found");
        provider.put("status", "DISABLED");
        provider.put("updatedBy", actor);
        return copy(provider);
    }

    public List<Map<String, Object>> providerModels(Long tenantId, Long id) {
        provider(tenantId, id);
        return List.of(
                Map.of("tenantId", tenantId, "providerId", id, "modelKey", "gpt-4.1-mini"),
                Map.of("tenantId", tenantId, "providerId", id, "modelKey", "gpt-4.1"));
    }

    private static Map<String, Object> recommendation(Long tenantId, Long id, String type, String status,
                                                      double score) {
        return Map.of("tenantId", tenantId, "recommendationId", id, "decisionType", type,
                "eligibilityStatus", status, "score", score);
    }

    private static Map<String, Object> riskUser(Long tenantId, Long userId, double score) {
        return Map.of("tenantId", tenantId, "userId", userId, "churnRiskScore", score);
    }

    private static Map<String, Object> latest(List<Map<String, Object>> records, String expected,
                                              String key, String message) {
        for (int index = records.size() - 1; index >= 0; index--) {
            Map<String, Object> record = records.get(index);
            if (expected == null || Objects.equals(record.get(key), expected)) {
                return copy(record);
            }
        }
        throw new IllegalArgumentException(message);
    }

    private static Map<String, Object> find(Map<Long, Map<Long, Map<String, Object>>> records, Long tenantId,
                                            Long id, String message) {
        Map<String, Object> record = records.getOrDefault(tenantId, Map.of()).get(id);
        if (record == null) {
            throw new IllegalArgumentException(message);
        }
        return record;
    }

    private static boolean matches(Map<String, Object> item, String key, String expected) {
        return expected == null || Objects.equals(item.get(key), expected);
    }

    private static Long nextId(Map<Long, Map<String, Object>> records) {
        return 7001L + records.size();
    }

    private static String requiredString(Map<String, Object> payload, String key) {
        String value = stringValue(payload.get(key), null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    private static String stringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    private static Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        return Long.valueOf(String.valueOf(value));
    }

    private static Map<String, Object> copy(Map<String, Object> record) {
        return new LinkedHashMap<>(record);
    }
}
