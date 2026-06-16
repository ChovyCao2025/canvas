package org.chovy.canvas.platform.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AI 能力目录，保存决策、预测、提示词模板、评估审计和供应方演示数据。
 */
public class AiCatalog {

    /**
     * 按租户保存的决策运行记录。
     */
    private final Map<Long, List<Map<String, Object>>> decisionRunsByTenant = new LinkedHashMap<>();

    /**
     * 按租户保存的预测运行记录。
     */
    private final Map<Long, List<Map<String, Object>>> predictionRunsByTenant = new LinkedHashMap<>();

    /**
     * 按租户和模板标识保存的提示词模板。
     */
    private final Map<Long, Map<Long, Map<String, Object>>> promptTemplatesByTenant = new LinkedHashMap<>();

    /**
     * 按租户保存的提示词评估审计。
     */
    private final Map<Long, List<Map<String, Object>>> evaluationAuditsByTenant = new LinkedHashMap<>();

    /**
     * 按租户和供应方标识保存的模型供应方。
     */
    private final Map<Long, Map<Long, Map<String, Object>>> providersByTenant = new LinkedHashMap<>();

    /**
     * 重新计算智能决策并记录运行。
     *
     * @param tenantId 租户标识
     * @param payload 运行参数
     * @param actor 操作者
     * @return 决策运行记录
     */
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

    /**
     * 查询指定范围的最新决策运行。
     *
     * @param tenantId 租户标识
     * @param decisionScope 决策范围
     * @return 最新决策运行记录
     */
    public Map<String, Object> latestDecisionRun(Long tenantId, String decisionScope) {
        return latest(decisionRunsByTenant.getOrDefault(tenantId, List.of()), decisionScope, "decisionScope",
                "decision run not found");
    }

    /**
     * 查询决策推荐项。
     *
     * @param tenantId 租户标识
     * @param runId 决策运行标识
     * @param decisionType 决策类型
     * @param eligibilityStatus 资格状态
     * @param limit 最大返回数量
     * @return 推荐项列表
     */
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

    /**
     * 记录决策推荐反馈。
     *
     * @param tenantId 租户标识
     * @param recommendationId 推荐项标识
     * @param payload 反馈参数
     * @param actor 操作者
     * @return 反馈记录
     */
    public Map<String, Object> recordDecisionFeedback(Long tenantId, Long recommendationId,
                                                      Map<String, Object> payload, String actor) {
        Map<String, Object> feedback = new LinkedHashMap<>(payload);
        feedback.put("tenantId", tenantId);
        feedback.put("recommendationId", recommendationId);
        feedback.put("feedbackStatus", "RECORDED");
        feedback.put("updatedBy", actor);
        return feedback;
    }

    /**
     * 查询最新流失预测运行。
     *
     * @param tenantId 租户标识
     * @return 最新预测运行记录；没有运行时返回 NOT_STARTED
     */
    public Map<String, Object> latestPredictionRun(Long tenantId) {
        List<Map<String, Object>> runs = predictionRunsByTenant.getOrDefault(tenantId, List.of());
        if (runs.isEmpty()) {
            return Map.of("tenantId", tenantId, "status", "NOT_STARTED");
        }
        return copy(runs.get(runs.size() - 1));
    }

    /**
     * 查询流失预测数据就绪状态。
     *
     * @param tenantId 租户标识
     * @return 预测就绪状态记录
     */
    public Map<String, Object> predictionReadiness(Long tenantId) {
        return Map.of("tenantId", tenantId, "ready", true, "requiredSignals", 3, "availableSignals", 3);
    }

    /**
     * 查询流失风险分布。
     *
     * @param tenantId 租户标识
     * @return 风险分布列表
     */
    public List<Map<String, Object>> churnDistribution(Long tenantId) {
        return List.of(
                Map.of("tenantId", tenantId, "bucket", "LOW", "users", 120),
                Map.of("tenantId", tenantId, "bucket", "MEDIUM", "users", 48),
                Map.of("tenantId", tenantId, "bucket", "HIGH", "users", 17));
    }

    /**
     * 查询高风险用户。
     *
     * @param tenantId 租户标识
     * @param limit 最大返回数量
     * @return 高风险用户列表
     */
    public List<Map<String, Object>> topRiskUsers(Long tenantId, int limit) {
        return List.of(
                riskUser(tenantId, 9001L, 0.94),
                riskUser(tenantId, 9002L, 0.89),
                riskUser(tenantId, 9003L, 0.83)).stream()
                .limit(limit)
                .map(AiCatalog::copy)
                .toList();
    }

    /**
     * 重新计算流失预测并记录运行。
     *
     * @param tenantId 租户标识
     * @param payload 运行参数
     * @return 预测运行记录
     */
    public Map<String, Object> recomputePrediction(Long tenantId, Map<String, Object> payload) {
        List<Map<String, Object>> runs = predictionRunsByTenant.computeIfAbsent(tenantId, ignored -> new ArrayList<>());
        Map<String, Object> run = new LinkedHashMap<>(payload);
        run.put("runId", "ai-prediction-" + tenantId + "-" + (runs.size() + 1));
        run.put("tenantId", tenantId);
        run.put("status", "COMPLETED");
        runs.add(run);
        return copy(run);
    }

    /**
     * 查询提示词模板。
     *
     * @param tenantId 租户标识
     * @return 提示词模板列表
     */
    public List<Map<String, Object>> promptTemplates(Long tenantId) {
        return promptTemplatesByTenant.getOrDefault(tenantId, Map.of()).values().stream()
                .map(AiCatalog::copy)
                .toList();
    }

    /**
     * 创建提示词模板。
     *
     * @param tenantId 租户标识
     * @param payload 模板参数
     * @param actor 操作者
     * @return 创建后的模板记录
     */
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

    /**
     * 查询单个提示词模板。
     *
     * @param tenantId 租户标识
     * @param id 模板标识
     * @return 提示词模板记录
     */
    public Map<String, Object> promptTemplate(Long tenantId, Long id) {
        return copy(find(promptTemplatesByTenant, tenantId, id, "prompt template not found"));
    }

    /**
     * 更新提示词模板。
     *
     * @param tenantId 租户标识
     * @param id 模板标识
     * @param payload 模板更新参数
     * @param actor 操作者
     * @return 更新后的模板记录
     */
    public Map<String, Object> updatePromptTemplate(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        Map<String, Object> template = find(promptTemplatesByTenant, tenantId, id, "prompt template not found");
        template.putAll(payload);
        template.put("updatedBy", actor);
        return copy(template);
    }

    /**
     * 禁用提示词模板。
     *
     * @param tenantId 租户标识
     * @param id 模板标识
     * @param actor 操作者
     * @return 禁用后的模板记录
     */
    public Map<String, Object> disablePromptTemplate(Long tenantId, Long id, String actor) {
        Map<String, Object> template = find(promptTemplatesByTenant, tenantId, id, "prompt template not found");
        template.put("status", "DISABLED");
        template.put("updatedBy", actor);
        return copy(template);
    }

    /**
     * 渲染提示词模板。
     *
     * @param tenantId 租户标识
     * @param payload 渲染参数
     * @return 渲染结果
     */
    public Map<String, Object> renderPromptTemplate(Long tenantId, Map<String, Object> payload) {
        Long templateId = longValue(payload.get("templateId"));
        Map<String, Object> template = promptTemplate(tenantId, templateId);
        String rendered = String.valueOf(template.getOrDefault("template", ""));
        Object variables = payload.get("variables");
        if (variables instanceof Map<?, ?> values) {
            // 使用简单占位替换满足本地演示，不引入模板引擎依赖。
            for (Map.Entry<?, ?> entry : values.entrySet()) {
                rendered = rendered.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
            }
        }
        return Map.of("tenantId", tenantId, "templateId", templateId, "renderedPrompt", rendered);
    }

    /**
     * 评估提示词模板并写入审计记录。
     *
     * @param tenantId 租户标识
     * @param payload 评估参数
     * @return 评估审计记录
     */
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

    /**
     * 查询提示词评估审计。
     *
     * @param tenantId 租户标识
     * @return 评估审计列表
     */
    public List<Map<String, Object>> evaluationAudits(Long tenantId) {
        return evaluationAuditsByTenant.getOrDefault(tenantId, List.of()).stream()
                .map(AiCatalog::copy)
                .toList();
    }

    /**
     * 查询模型供应方。
     *
     * @param tenantId 租户标识
     * @return 供应方列表
     */
    public List<Map<String, Object>> providers(Long tenantId) {
        return providersByTenant.getOrDefault(tenantId, Map.of()).values().stream()
                .map(AiCatalog::copy)
                .toList();
    }

    /**
     * 创建模型供应方。
     *
     * @param tenantId 租户标识
     * @param payload 供应方参数
     * @param actor 操作者
     * @return 创建后的供应方记录
     */
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

    /**
     * 查询单个模型供应方。
     *
     * @param tenantId 租户标识
     * @param id 供应方标识
     * @return 供应方记录
     */
    public Map<String, Object> provider(Long tenantId, Long id) {
        return copy(find(providersByTenant, tenantId, id, "provider not found"));
    }

    /**
     * 更新模型供应方。
     *
     * @param tenantId 租户标识
     * @param id 供应方标识
     * @param payload 供应方更新参数
     * @param actor 操作者
     * @return 更新后的供应方记录
     */
    public Map<String, Object> updateProvider(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        Map<String, Object> provider = find(providersByTenant, tenantId, id, "provider not found");
        provider.putAll(payload);
        provider.put("updatedBy", actor);
        return copy(provider);
    }

    /**
     * 禁用模型供应方。
     *
     * @param tenantId 租户标识
     * @param id 供应方标识
     * @param actor 操作者
     * @return 禁用后的供应方记录
     */
    public Map<String, Object> disableProvider(Long tenantId, Long id, String actor) {
        Map<String, Object> provider = find(providersByTenant, tenantId, id, "provider not found");
        provider.put("status", "DISABLED");
        provider.put("updatedBy", actor);
        return copy(provider);
    }

    /**
     * 查询供应方模型。
     *
     * @param tenantId 租户标识
     * @param id 供应方标识
     * @return 模型列表
     */
    public List<Map<String, Object>> providerModels(Long tenantId, Long id) {
        provider(tenantId, id);
        return List.of(
                Map.of("tenantId", tenantId, "providerId", id, "modelKey", "gpt-4.1-mini"),
                Map.of("tenantId", tenantId, "providerId", id, "modelKey", "gpt-4.1"));
    }

    /**
     * 构造决策推荐项。
     *
     * @param tenantId 租户标识
     * @param id 推荐项标识
     * @param type 决策类型
     * @param status 资格状态
     * @param score 推荐分数
     * @return 推荐项记录
     */
    private static Map<String, Object> recommendation(Long tenantId, Long id, String type, String status,
                                                      double score) {
        return Map.of("tenantId", tenantId, "recommendationId", id, "decisionType", type,
                "eligibilityStatus", status, "score", score);
    }

    /**
     * 构造高风险用户记录。
     *
     * @param tenantId 租户标识
     * @param userId 用户标识
     * @param score 流失风险分数
     * @return 高风险用户记录
     */
    private static Map<String, Object> riskUser(Long tenantId, Long userId, double score) {
        return Map.of("tenantId", tenantId, "userId", userId, "churnRiskScore", score);
    }

    /**
     * 从列表尾部查找最新匹配记录。
     *
     * @param records 记录列表
     * @param expected 期望值
     * @param key 匹配字段
     * @param message 未找到时使用的异常消息
     * @return 最新匹配记录
     */
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

    /**
     * 在租户级记录表中查找单条记录。
     *
     * @param records 租户级记录表
     * @param tenantId 租户标识
     * @param id 记录标识
     * @param message 未找到时使用的异常消息
     * @return 匹配记录
     */
    private static Map<String, Object> find(Map<Long, Map<Long, Map<String, Object>>> records, Long tenantId,
                                            Long id, String message) {
        Map<String, Object> record = records.getOrDefault(tenantId, Map.of()).get(id);
        if (record == null) {
            throw new IllegalArgumentException(message);
        }
        return record;
    }

    /**
     * 判断记录字段是否匹配可选过滤值。
     *
     * @param item 记录
     * @param key 字段键
     * @param expected 期望值
     * @return 匹配或未提供期望值时返回 true
     */
    private static boolean matches(Map<String, Object> item, String key, String expected) {
        return expected == null || Objects.equals(item.get(key), expected);
    }

    /**
     * 根据已有记录数量生成下一个标识。
     *
     * @param records 已有记录表
     * @return 下一个标识
     */
    private static Long nextId(Map<Long, Map<String, Object>> records) {
        return 7001L + records.size();
    }

    /**
     * 读取必填字符串字段。
     *
     * @param payload 请求体
     * @param key 字段键
     * @return 字段值
     */
    private static String requiredString(Map<String, Object> payload, String key) {
        String value = stringValue(payload.get(key), null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    /**
     * 将对象转为字符串并提供默认值。
     *
     * @param value 原始对象
     * @param defaultValue 默认值
     * @return 字符串值
     */
    private static String stringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    /**
     * 将对象转为 Long。
     *
     * @param value 原始对象
     * @return Long 值；空值返回 null
     */
    private static Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        return Long.valueOf(String.valueOf(value));
    }

    /**
     * 复制记录，避免调用方直接修改内部状态。
     *
     * @param record 原始记录
     * @return 复制后的记录
     */
    private static Map<String, Object> copy(Map<String, Object> record) {
        return new LinkedHashMap<>(record);
    }
}
