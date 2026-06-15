package org.chovy.canvas.marketing.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.SearchMarketingFacade;
import org.chovy.canvas.marketing.domain.SearchMarketingCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchMarketingApplicationService implements SearchMarketingFacade {

    private final SearchMarketingCatalog catalog;

    public SearchMarketingApplicationService() {
        this(Clock.systemDefaultZone());
    }

    SearchMarketingApplicationService(Clock clock) {
        this.catalog = new SearchMarketingCatalog(clock);
    }

    @Override
    public List<Map<String, Object>> listSources(Long tenantId, String provider, String channel,
                                                 Boolean enabled, Integer limit) {
        return catalog.listSources(safeTenantId(tenantId), provider, channel, enabled, normalizedLimit(limit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertSource(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertSource(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> listKeywords(Long tenantId, String channel, String status, Integer limit) {
        return catalog.listKeywords(safeTenantId(tenantId), channel, status, normalizedLimit(limit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertKeyword(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertKeyword(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> listSnapshots(Long tenantId, String channel, Long sourceId, Long keywordId,
                                                   LocalDate startDate, LocalDate endDate, Integer limit) {
        return catalog.listSnapshots(safeTenantId(tenantId), channel, sourceId, keywordId, startDate, endDate,
                normalizedLimit(limit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertSnapshot(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertSnapshot(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> listOpportunities(Long tenantId, String channel, Long sourceId,
                                                       String status, String severity, Integer limit) {
        return catalog.listOpportunities(safeTenantId(tenantId), channel, sourceId, status, severity,
                normalizedLimit(limit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> evaluateOpportunities(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.evaluateOpportunities(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateOpportunityStatus(Long tenantId, Long opportunityId, Map<String, Object> payload,
                                                       String actor) {
        return catalog.updateOpportunityStatus(safeTenantId(tenantId), opportunityId, payload, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createOpportunityMutation(Long tenantId, Long opportunityId,
                                                         Map<String, Object> payload, String actor) {
        return catalog.createMutation(safeTenantId(tenantId), opportunityId, payload, actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> listMutations(Long tenantId, Long sourceId, String status,
                                                   String approvalStatus, Integer limit) {
        return catalog.listMutations(safeTenantId(tenantId), sourceId, status, approvalStatus, normalizedLimit(limit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertMutation(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createMutation(safeTenantId(tenantId), null, payload, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> approveMutation(Long tenantId, Long mutationId, Map<String, Object> payload,
                                               String actor) {
        return catalog.approveMutation(safeTenantId(tenantId), mutationId, payload, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> executeMutation(Long tenantId, Long mutationId, Map<String, Object> payload,
                                               String actor) {
        return catalog.executeMutation(safeTenantId(tenantId), mutationId, payload, actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> listUrlInspections(Long tenantId, Long sourceId, String indexedState,
                                                        LocalDate startDate, LocalDate endDate, Integer limit) {
        return catalog.listUrlInspections(safeTenantId(tenantId), sourceId, indexedState, startDate, endDate,
                normalizedLimit(limit));
    }

    @Override
    public List<Map<String, Object>> listSyncRuns(Long tenantId, Long sourceId, String runType, String status,
                                                  Integer limit) {
        return catalog.listSyncRuns(safeTenantId(tenantId), sourceId, runType, status, normalizedLimit(limit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> syncSource(Long tenantId, Long sourceId, Map<String, Object> payload, String actor) {
        return catalog.syncSource(safeTenantId(tenantId), sourceId, payload, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> syncDue(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.syncDue(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> listProviderChanges(Long tenantId, Long sourceId, Long mutationId,
                                                         String provider, String reconciliationStatus, Integer limit) {
        return catalog.listProviderChanges(safeTenantId(tenantId), sourceId, mutationId, provider,
                reconciliationStatus, normalizedLimit(limit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reconcileMutation(Long tenantId, Long mutationId, String actor) {
        return catalog.reconcileMutation(safeTenantId(tenantId), mutationId, actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> listImpactWindows(Long tenantId, Long opportunityId, Long mutationId,
                                                       Long sourceId, String status, String decision, Integer limit) {
        return catalog.listImpactWindows(safeTenantId(tenantId), opportunityId, mutationId, sourceId, status,
                decision, normalizedLimit(limit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> evaluateDueImpactWindows(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.evaluateDueImpactWindows(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    @Override
    public Map<String, Object> readiness(Long tenantId) {
        return catalog.readiness(safeTenantId(tenantId));
    }

    @Override
    public Map<String, Object> summary(Long tenantId, String channel, Long sourceId, Long keywordId,
                                       LocalDate startDate, LocalDate endDate) {
        return catalog.summary(safeTenantId(tenantId), channel, sourceId, keywordId, startDate, endDate);
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
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
