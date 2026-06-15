package org.chovy.canvas.platform.api;

import java.util.List;
import java.util.Map;

public interface AiFacade {

    Map<String, Object> recomputeDecision(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> latestDecisionRun(Long tenantId, String decisionScope);

    List<Map<String, Object>> decisionRecommendations(Long tenantId, Long runId, String decisionType,
                                                       String eligibilityStatus, Integer limit);

    Map<String, Object> recordDecisionFeedback(Long tenantId, Long recommendationId, Map<String, Object> payload,
                                               String actor);

    Map<String, Object> latestPredictionRun(Long tenantId);

    Map<String, Object> predictionReadiness(Long tenantId);

    List<Map<String, Object>> churnDistribution(Long tenantId);

    List<Map<String, Object>> topRiskUsers(Long tenantId, Integer limit);

    Map<String, Object> recomputePrediction(Long tenantId, Map<String, Object> payload);

    List<Map<String, Object>> promptTemplates(Long tenantId);

    Map<String, Object> createPromptTemplate(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> promptTemplate(Long tenantId, Long id);

    Map<String, Object> updatePromptTemplate(Long tenantId, Long id, Map<String, Object> payload, String actor);

    Map<String, Object> disablePromptTemplate(Long tenantId, Long id, String actor);

    Map<String, Object> renderPromptTemplate(Long tenantId, Map<String, Object> payload);

    Map<String, Object> evaluatePromptTemplate(Long tenantId, Map<String, Object> payload);

    List<Map<String, Object>> evaluationAudits(Long tenantId);

    List<Map<String, Object>> providers(Long tenantId);

    Map<String, Object> createProvider(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> provider(Long tenantId, Long id);

    Map<String, Object> updateProvider(Long tenantId, Long id, Map<String, Object> payload, String actor);

    Map<String, Object> disableProvider(Long tenantId, Long id, String actor);

    List<Map<String, Object>> providerModels(Long tenantId, Long id);
}
