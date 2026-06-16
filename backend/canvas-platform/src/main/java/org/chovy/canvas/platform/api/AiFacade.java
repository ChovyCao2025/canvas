package org.chovy.canvas.platform.api;

import java.util.List;
import java.util.Map;

/**
 * 提供智能决策、流失预测、提示词模板和模型供应方能力的应用入口。
 */
public interface AiFacade {

    /**
     * 重新计算智能决策结果。
     *
     * @param tenantId 租户标识
     * @param payload 重新计算参数
     * @param actor 操作者
     * @return 决策运行记录
     */
    Map<String, Object> recomputeDecision(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 查询指定决策范围的最新运行记录。
     *
     * @param tenantId 租户标识
     * @param decisionScope 决策范围
     * @return 最新决策运行记录
     */
    Map<String, Object> latestDecisionRun(Long tenantId, String decisionScope);

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
    List<Map<String, Object>> decisionRecommendations(Long tenantId, Long runId, String decisionType,
                                                       String eligibilityStatus, Integer limit);

    /**
     * 记录决策推荐反馈。
     *
     * @param tenantId 租户标识
     * @param recommendationId 推荐项标识
     * @param payload 反馈参数
     * @param actor 操作者
     * @return 反馈记录
     */
    Map<String, Object> recordDecisionFeedback(Long tenantId, Long recommendationId, Map<String, Object> payload,
                                               String actor);

    /**
     * 查询最新流失预测运行记录。
     *
     * @param tenantId 租户标识
     * @return 最新预测运行记录
     */
    Map<String, Object> latestPredictionRun(Long tenantId);

    /**
     * 查询流失预测的数据就绪状态。
     *
     * @param tenantId 租户标识
     * @return 预测就绪状态记录
     */
    Map<String, Object> predictionReadiness(Long tenantId);

    /**
     * 查询流失风险分布。
     *
     * @param tenantId 租户标识
     * @return 流失风险分布列表
     */
    List<Map<String, Object>> churnDistribution(Long tenantId);

    /**
     * 查询高风险用户。
     *
     * @param tenantId 租户标识
     * @param limit 最大返回数量
     * @return 高风险用户列表
     */
    List<Map<String, Object>> topRiskUsers(Long tenantId, Integer limit);

    /**
     * 重新计算流失预测结果。
     *
     * @param tenantId 租户标识
     * @param payload 重新计算参数
     * @return 预测运行记录
     */
    Map<String, Object> recomputePrediction(Long tenantId, Map<String, Object> payload);

    /**
     * 查询提示词模板。
     *
     * @param tenantId 租户标识
     * @return 提示词模板列表
     */
    List<Map<String, Object>> promptTemplates(Long tenantId);

    /**
     * 创建提示词模板。
     *
     * @param tenantId 租户标识
     * @param payload 模板创建参数
     * @param actor 操作者
     * @return 创建后的模板记录
     */
    Map<String, Object> createPromptTemplate(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 查询单个提示词模板。
     *
     * @param tenantId 租户标识
     * @param id 模板标识
     * @return 提示词模板记录
     */
    Map<String, Object> promptTemplate(Long tenantId, Long id);

    /**
     * 更新提示词模板。
     *
     * @param tenantId 租户标识
     * @param id 模板标识
     * @param payload 模板更新参数
     * @param actor 操作者
     * @return 更新后的模板记录
     */
    Map<String, Object> updatePromptTemplate(Long tenantId, Long id, Map<String, Object> payload, String actor);

    /**
     * 禁用提示词模板。
     *
     * @param tenantId 租户标识
     * @param id 模板标识
     * @param actor 操作者
     * @return 禁用后的模板记录
     */
    Map<String, Object> disablePromptTemplate(Long tenantId, Long id, String actor);

    /**
     * 渲染提示词模板。
     *
     * @param tenantId 租户标识
     * @param payload 渲染参数
     * @return 渲染结果
     */
    Map<String, Object> renderPromptTemplate(Long tenantId, Map<String, Object> payload);

    /**
     * 评估提示词模板。
     *
     * @param tenantId 租户标识
     * @param payload 评估参数
     * @return 评估结果
     */
    Map<String, Object> evaluatePromptTemplate(Long tenantId, Map<String, Object> payload);

    /**
     * 查询提示词评估审计记录。
     *
     * @param tenantId 租户标识
     * @return 评估审计列表
     */
    List<Map<String, Object>> evaluationAudits(Long tenantId);

    /**
     * 查询模型供应方。
     *
     * @param tenantId 租户标识
     * @return 供应方列表
     */
    List<Map<String, Object>> providers(Long tenantId);

    /**
     * 创建模型供应方。
     *
     * @param tenantId 租户标识
     * @param payload 供应方创建参数
     * @param actor 操作者
     * @return 创建后的供应方记录
     */
    Map<String, Object> createProvider(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 查询单个模型供应方。
     *
     * @param tenantId 租户标识
     * @param id 供应方标识
     * @return 供应方记录
     */
    Map<String, Object> provider(Long tenantId, Long id);

    /**
     * 更新模型供应方。
     *
     * @param tenantId 租户标识
     * @param id 供应方标识
     * @param payload 供应方更新参数
     * @param actor 操作者
     * @return 更新后的供应方记录
     */
    Map<String, Object> updateProvider(Long tenantId, Long id, Map<String, Object> payload, String actor);

    /**
     * 禁用模型供应方。
     *
     * @param tenantId 租户标识
     * @param id 供应方标识
     * @param actor 操作者
     * @return 禁用后的供应方记录
     */
    Map<String, Object> disableProvider(Long tenantId, Long id, String actor);

    /**
     * 查询模型供应方支持的模型。
     *
     * @param tenantId 租户标识
     * @param id 供应方标识
     * @return 供应方模型列表
     */
    List<Map<String, Object>> providerModels(Long tenantId, Long id);
}
