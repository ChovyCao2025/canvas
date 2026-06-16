package org.chovy.canvas.marketing.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.SearchMarketingFacade;
import org.chovy.canvas.marketing.domain.SearchMarketingCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排SearchMarketing相关的应用层用例。
 */
@Service
public class SearchMarketingApplicationService implements SearchMarketingFacade {

    /**
     * 承载该应用服务的内存目录。
     */
    private final SearchMarketingCatalog catalog;

    /**
     * 创建SearchMarketingApplicationService实例。
     */
    public SearchMarketingApplicationService() {
        this(Clock.systemDefaultZone());
    }

    SearchMarketingApplicationService(Clock clock) {
        this.catalog = new SearchMarketingCatalog(clock);
    }

    /**
     * 查询sources列表。
     */
    @Override
    public List<Map<String, Object>> listSources(Long tenantId, String provider, String channel,
                                                 Boolean enabled, Integer limit) {
        return catalog.listSources(safeTenantId(tenantId), provider, channel, enabled, normalizedLimit(limit));
    }

    /**
     * 执行upsertSource业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertSource(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertSource(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    /**
     * 查询keywords列表。
     */
    @Override
    public List<Map<String, Object>> listKeywords(Long tenantId, String channel, String status, Integer limit) {
        return catalog.listKeywords(safeTenantId(tenantId), channel, status, normalizedLimit(limit));
    }

    /**
     * 执行upsertKeyword业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertKeyword(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertKeyword(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    /**
     * 查询snapshots列表。
     */
    @Override
    public List<Map<String, Object>> listSnapshots(Long tenantId, String channel, Long sourceId, Long keywordId,
                                                   LocalDate startDate, LocalDate endDate, Integer limit) {
        return catalog.listSnapshots(safeTenantId(tenantId), channel, sourceId, keywordId, startDate, endDate,
                normalizedLimit(limit));
    }

    /**
     * 执行upsertSnapshot业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertSnapshot(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertSnapshot(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    /**
     * 查询opportunities列表。
     */
    @Override
    public List<Map<String, Object>> listOpportunities(Long tenantId, String channel, Long sourceId,
                                                       String status, String severity, Integer limit) {
        return catalog.listOpportunities(safeTenantId(tenantId), channel, sourceId, status, severity,
                normalizedLimit(limit));
    }

    /**
     * 执行evaluateOpportunities业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> evaluateOpportunities(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.evaluateOpportunities(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    /**
     * 更新opportunityStatus业务对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateOpportunityStatus(Long tenantId, Long opportunityId, Map<String, Object> payload,
                                                       String actor) {
        return catalog.updateOpportunityStatus(safeTenantId(tenantId), opportunityId, payload, actorOrDefault(actor));
    }

    /**
     * 创建opportunityMutation业务对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createOpportunityMutation(Long tenantId, Long opportunityId,
                                                         Map<String, Object> payload, String actor) {
        return catalog.createMutation(safeTenantId(tenantId), opportunityId, payload, actorOrDefault(actor));
    }

    /**
     * 查询mutations列表。
     */
    @Override
    public List<Map<String, Object>> listMutations(Long tenantId, Long sourceId, String status,
                                                   String approvalStatus, Integer limit) {
        return catalog.listMutations(safeTenantId(tenantId), sourceId, status, approvalStatus, normalizedLimit(limit));
    }

    /**
     * 执行upsertMutation业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertMutation(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createMutation(safeTenantId(tenantId), null, payload, actorOrDefault(actor));
    }

    /**
     * 执行approveMutation业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> approveMutation(Long tenantId, Long mutationId, Map<String, Object> payload,
                                               String actor) {
        return catalog.approveMutation(safeTenantId(tenantId), mutationId, payload, actorOrDefault(actor));
    }

    /**
     * 执行executeMutation业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> executeMutation(Long tenantId, Long mutationId, Map<String, Object> payload,
                                               String actor) {
        return catalog.executeMutation(safeTenantId(tenantId), mutationId, payload, actorOrDefault(actor));
    }

    /**
     * 查询urlInspections列表。
     */
    @Override
    public List<Map<String, Object>> listUrlInspections(Long tenantId, Long sourceId, String indexedState,
                                                        LocalDate startDate, LocalDate endDate, Integer limit) {
        return catalog.listUrlInspections(safeTenantId(tenantId), sourceId, indexedState, startDate, endDate,
                normalizedLimit(limit));
    }

    /**
     * 查询syncRuns列表。
     */
    @Override
    public List<Map<String, Object>> listSyncRuns(Long tenantId, Long sourceId, String runType, String status,
                                                  Integer limit) {
        return catalog.listSyncRuns(safeTenantId(tenantId), sourceId, runType, status, normalizedLimit(limit));
    }

    /**
     * 执行syncSource业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> syncSource(Long tenantId, Long sourceId, Map<String, Object> payload, String actor) {
        return catalog.syncSource(safeTenantId(tenantId), sourceId, payload, actorOrDefault(actor));
    }

    /**
     * 执行syncDue业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> syncDue(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.syncDue(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    /**
     * 查询providerChanges列表。
     */
    @Override
    public List<Map<String, Object>> listProviderChanges(Long tenantId, Long sourceId, Long mutationId,
                                                         String provider, String reconciliationStatus, Integer limit) {
        return catalog.listProviderChanges(safeTenantId(tenantId), sourceId, mutationId, provider,
                reconciliationStatus, normalizedLimit(limit));
    }

    /**
     * 执行reconcileMutation业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reconcileMutation(Long tenantId, Long mutationId, String actor) {
        return catalog.reconcileMutation(safeTenantId(tenantId), mutationId, actorOrDefault(actor));
    }

    /**
     * 查询impactWindows列表。
     */
    @Override
    public List<Map<String, Object>> listImpactWindows(Long tenantId, Long opportunityId, Long mutationId,
                                                       Long sourceId, String status, String decision, Integer limit) {
        return catalog.listImpactWindows(safeTenantId(tenantId), opportunityId, mutationId, sourceId, status,
                decision, normalizedLimit(limit));
    }

    /**
     * 执行evaluateDueImpactWindows业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> evaluateDueImpactWindows(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.evaluateDueImpactWindows(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    /**
     * 执行readiness业务操作。
     */
    @Override
    public Map<String, Object> readiness(Long tenantId) {
        return catalog.readiness(safeTenantId(tenantId));
    }

    /**
     * 执行summary业务操作。
     */
    @Override
    public Map<String, Object> summary(Long tenantId, String channel, Long sourceId, Long keywordId,
                                       LocalDate startDate, LocalDate endDate) {
        return catalog.summary(safeTenantId(tenantId), channel, sourceId, keywordId, startDate, endDate);
    }

    /**
     * 执行safeTenantId业务操作。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 执行actorOrDefault业务操作。
     */
    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    /**
     * 规范化dLimit输入值。
     */
    private static int normalizedLimit(Integer limit) {
        if (limit == null) {
            return 50;
        }
        return Math.max(1, Math.min(limit, 100));
    }
}
