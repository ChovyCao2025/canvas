package org.chovy.canvas.platform.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.api.AiFacade;
import org.chovy.canvas.platform.domain.AiCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiApplicationService implements AiFacade {

    private final AiCatalog catalog;

    public AiApplicationService() {
        this(new AiCatalog());
    }

    public AiApplicationService(AiCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> recomputeDecision(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.recomputeDecision(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    @Override
    public Map<String, Object> latestDecisionRun(Long tenantId, String decisionScope) {
        return catalog.latestDecisionRun(safeTenantId(tenantId), decisionScope);
    }

    @Override
    public List<Map<String, Object>> decisionRecommendations(Long tenantId, Long runId, String decisionType,
                                                             String eligibilityStatus, Integer limit) {
        return catalog.decisionRecommendations(safeTenantId(tenantId), runId, decisionType, eligibilityStatus,
                normalizedLimit(limit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> recordDecisionFeedback(Long tenantId, Long recommendationId,
                                                      Map<String, Object> payload, String actor) {
        return catalog.recordDecisionFeedback(safeTenantId(tenantId), recommendationId, safePayload(payload),
                actorOrDefault(actor));
    }

    @Override
    public Map<String, Object> latestPredictionRun(Long tenantId) {
        return catalog.latestPredictionRun(safeTenantId(tenantId));
    }

    @Override
    public Map<String, Object> predictionReadiness(Long tenantId) {
        return catalog.predictionReadiness(safeTenantId(tenantId));
    }

    @Override
    public List<Map<String, Object>> churnDistribution(Long tenantId) {
        return catalog.churnDistribution(safeTenantId(tenantId));
    }

    @Override
    public List<Map<String, Object>> topRiskUsers(Long tenantId, Integer limit) {
        return catalog.topRiskUsers(safeTenantId(tenantId), normalizedLimit(limit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> recomputePrediction(Long tenantId, Map<String, Object> payload) {
        return catalog.recomputePrediction(safeTenantId(tenantId), safePayload(payload));
    }

    @Override
    public List<Map<String, Object>> promptTemplates(Long tenantId) {
        return catalog.promptTemplates(safeTenantId(tenantId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createPromptTemplate(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createPromptTemplate(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    @Override
    public Map<String, Object> promptTemplate(Long tenantId, Long id) {
        return catalog.promptTemplate(safeTenantId(tenantId), id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updatePromptTemplate(Long tenantId, Long id, Map<String, Object> payload,
                                                    String actor) {
        return catalog.updatePromptTemplate(safeTenantId(tenantId), id, safePayload(payload), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> disablePromptTemplate(Long tenantId, Long id, String actor) {
        return catalog.disablePromptTemplate(safeTenantId(tenantId), id, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> renderPromptTemplate(Long tenantId, Map<String, Object> payload) {
        return catalog.renderPromptTemplate(safeTenantId(tenantId), safePayload(payload));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> evaluatePromptTemplate(Long tenantId, Map<String, Object> payload) {
        return catalog.evaluatePromptTemplate(safeTenantId(tenantId), safePayload(payload));
    }

    @Override
    public List<Map<String, Object>> evaluationAudits(Long tenantId) {
        return catalog.evaluationAudits(safeTenantId(tenantId));
    }

    @Override
    public List<Map<String, Object>> providers(Long tenantId) {
        return catalog.providers(safeTenantId(tenantId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createProvider(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createProvider(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    @Override
    public Map<String, Object> provider(Long tenantId, Long id) {
        return catalog.provider(safeTenantId(tenantId), id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateProvider(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return catalog.updateProvider(safeTenantId(tenantId), id, safePayload(payload), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> disableProvider(Long tenantId, Long id, String actor) {
        return catalog.disableProvider(safeTenantId(tenantId), id, actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> providerModels(Long tenantId, Long id) {
        return catalog.providerModels(safeTenantId(tenantId), id);
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    private static int normalizedLimit(Integer limit) {
        if (limit == null) {
            return 50;
        }
        return Math.max(1, Math.min(limit, 100));
    }
}
