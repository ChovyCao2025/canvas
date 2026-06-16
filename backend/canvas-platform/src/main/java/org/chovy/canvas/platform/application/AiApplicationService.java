package org.chovy.canvas.platform.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.api.AiFacade;
import org.chovy.canvas.platform.domain.AiCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 智能能力应用服务，负责把 AI 控制台请求委托给目录层。
 */
@Service
public class AiApplicationService implements AiFacade {

    /**
     * 保存决策、预测、提示词和供应方数据的目录。
     */
    private final AiCatalog catalog;

    /**
     * 使用默认内存目录创建智能能力应用服务。
     */
    public AiApplicationService() {
        this(new AiCatalog());
    }

    /**
     * 使用指定目录创建智能能力应用服务。
     *
     * @param catalog AI 能力目录
     */
    public AiApplicationService(AiCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 重新计算智能决策。
     *
     * @param tenantId 租户标识
     * @param payload 重新计算参数
     * @param actor 操作者
     * @return 决策运行记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> recomputeDecision(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.recomputeDecision(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 查询最新决策运行。
     *
     * @param tenantId 租户标识
     * @param decisionScope 决策范围
     * @return 最新决策运行记录
     */
    @Override
    public Map<String, Object> latestDecisionRun(Long tenantId, String decisionScope) {
        return catalog.latestDecisionRun(safeTenantId(tenantId), decisionScope);
    }

    /**
     * 查询决策推荐项。
     *
     * @param tenantId 租户标识
     * @param runId 决策运行标识
     * @param decisionType 决策类型
     * @param eligibilityStatus 资格状态过滤值
     * @param limit 最大返回数量
     * @return 决策推荐列表
     */
    @Override
    public List<Map<String, Object>> decisionRecommendations(Long tenantId, Long runId, String decisionType,
                                                             String eligibilityStatus, Integer limit) {
        return catalog.decisionRecommendations(safeTenantId(tenantId), runId, decisionType, eligibilityStatus,
                normalizedLimit(limit));
    }

    /**
     * 记录决策反馈。
     *
     * @param tenantId 租户标识
     * @param recommendationId 推荐项标识
     * @param payload 反馈参数
     * @param actor 操作者
     * @return 反馈记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> recordDecisionFeedback(Long tenantId, Long recommendationId,
                                                      Map<String, Object> payload, String actor) {
        return catalog.recordDecisionFeedback(safeTenantId(tenantId), recommendationId, safePayload(payload),
                actorOrDefault(actor));
    }

    /**
     * 查询最新流失预测运行。
     *
     * @param tenantId 租户标识
     * @return 最新预测运行记录
     */
    @Override
    public Map<String, Object> latestPredictionRun(Long tenantId) {
        return catalog.latestPredictionRun(safeTenantId(tenantId));
    }

    /**
     * 查询流失预测就绪状态。
     *
     * @param tenantId 租户标识
     * @return 预测就绪状态记录
     */
    @Override
    public Map<String, Object> predictionReadiness(Long tenantId) {
        return catalog.predictionReadiness(safeTenantId(tenantId));
    }

    /**
     * 查询流失风险分布。
     *
     * @param tenantId 租户标识
     * @return 流失风险分布列表
     */
    @Override
    public List<Map<String, Object>> churnDistribution(Long tenantId) {
        return catalog.churnDistribution(safeTenantId(tenantId));
    }

    /**
     * 查询高风险用户。
     *
     * @param tenantId 租户标识
     * @param limit 最大返回数量
     * @return 高风险用户列表
     */
    @Override
    public List<Map<String, Object>> topRiskUsers(Long tenantId, Integer limit) {
        return catalog.topRiskUsers(safeTenantId(tenantId), normalizedLimit(limit));
    }

    /**
     * 重新计算流失预测。
     *
     * @param tenantId 租户标识
     * @param payload 重新计算参数
     * @return 预测运行记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> recomputePrediction(Long tenantId, Map<String, Object> payload) {
        return catalog.recomputePrediction(safeTenantId(tenantId), safePayload(payload));
    }

    /**
     * 查询提示词模板。
     *
     * @param tenantId 租户标识
     * @return 提示词模板列表
     */
    @Override
    public List<Map<String, Object>> promptTemplates(Long tenantId) {
        return catalog.promptTemplates(safeTenantId(tenantId));
    }

    /**
     * 创建提示词模板。
     *
     * @param tenantId 租户标识
     * @param payload 模板创建参数
     * @param actor 操作者
     * @return 创建后的模板记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createPromptTemplate(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createPromptTemplate(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 查询单个提示词模板。
     *
     * @param tenantId 租户标识
     * @param id 模板标识
     * @return 提示词模板记录
     */
    @Override
    public Map<String, Object> promptTemplate(Long tenantId, Long id) {
        return catalog.promptTemplate(safeTenantId(tenantId), id);
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
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updatePromptTemplate(Long tenantId, Long id, Map<String, Object> payload,
                                                    String actor) {
        return catalog.updatePromptTemplate(safeTenantId(tenantId), id, safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 禁用提示词模板。
     *
     * @param tenantId 租户标识
     * @param id 模板标识
     * @param actor 操作者
     * @return 禁用后的模板记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> disablePromptTemplate(Long tenantId, Long id, String actor) {
        return catalog.disablePromptTemplate(safeTenantId(tenantId), id, actorOrDefault(actor));
    }

    /**
     * 渲染提示词模板。
     *
     * @param tenantId 租户标识
     * @param payload 渲染参数
     * @return 渲染结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> renderPromptTemplate(Long tenantId, Map<String, Object> payload) {
        return catalog.renderPromptTemplate(safeTenantId(tenantId), safePayload(payload));
    }

    /**
     * 评估提示词模板。
     *
     * @param tenantId 租户标识
     * @param payload 评估参数
     * @return 评估结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> evaluatePromptTemplate(Long tenantId, Map<String, Object> payload) {
        return catalog.evaluatePromptTemplate(safeTenantId(tenantId), safePayload(payload));
    }

    /**
     * 查询提示词评估审计。
     *
     * @param tenantId 租户标识
     * @return 评估审计列表
     */
    @Override
    public List<Map<String, Object>> evaluationAudits(Long tenantId) {
        return catalog.evaluationAudits(safeTenantId(tenantId));
    }

    /**
     * 查询模型供应方。
     *
     * @param tenantId 租户标识
     * @return 供应方列表
     */
    @Override
    public List<Map<String, Object>> providers(Long tenantId) {
        return catalog.providers(safeTenantId(tenantId));
    }

    /**
     * 创建模型供应方。
     *
     * @param tenantId 租户标识
     * @param payload 供应方创建参数
     * @param actor 操作者
     * @return 创建后的供应方记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createProvider(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createProvider(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 查询单个模型供应方。
     *
     * @param tenantId 租户标识
     * @param id 供应方标识
     * @return 供应方记录
     */
    @Override
    public Map<String, Object> provider(Long tenantId, Long id) {
        return catalog.provider(safeTenantId(tenantId), id);
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
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateProvider(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return catalog.updateProvider(safeTenantId(tenantId), id, safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 禁用模型供应方。
     *
     * @param tenantId 租户标识
     * @param id 供应方标识
     * @param actor 操作者
     * @return 禁用后的供应方记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> disableProvider(Long tenantId, Long id, String actor) {
        return catalog.disableProvider(safeTenantId(tenantId), id, actorOrDefault(actor));
    }

    /**
     * 查询供应方模型。
     *
     * @param tenantId 租户标识
     * @param id 供应方标识
     * @return 供应方模型列表
     */
    @Override
    public List<Map<String, Object>> providerModels(Long tenantId, Long id) {
        return catalog.providerModels(safeTenantId(tenantId), id);
    }

    /**
     * 将缺失或非法租户标识归一到默认租户。
     *
     * @param tenantId 原始租户标识
     * @return 可传递给目录层的租户标识
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 将空请求体归一为空 Map。
     *
     * @param payload 原始请求体
     * @return 非空请求体
     */
    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    /**
     * 将缺失操作者归一为系统操作者。
     *
     * @param actor 原始操作者
     * @return 可审计的操作者名称
     */
    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    /**
     * 将列表数量限制归一到平台允许范围。
     *
     * @param limit 原始限制数量
     * @return 1 到 100 之间的限制数量
     */
    private static int normalizedLimit(Integer limit) {
        if (limit == null) {
            return 50;
        }
        return Math.max(1, Math.min(limit, 100));
    }
}
