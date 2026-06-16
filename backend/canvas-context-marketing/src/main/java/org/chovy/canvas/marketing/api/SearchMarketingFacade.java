package org.chovy.canvas.marketing.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 定义SearchMarketingFacade的营销上下文访问契约。
 */
public interface SearchMarketingFacade {

    /**
     * 查询sources列表。
     */
    List<Map<String, Object>> listSources(Long tenantId, String provider, String channel, Boolean enabled, Integer limit);

    /**
     * 执行upsertSource业务操作。
     */
    Map<String, Object> upsertSource(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 查询keywords列表。
     */
    List<Map<String, Object>> listKeywords(Long tenantId, String channel, String status, Integer limit);

    /**
     * 执行upsertKeyword业务操作。
     */
    Map<String, Object> upsertKeyword(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 查询snapshots列表。
     */
    List<Map<String, Object>> listSnapshots(Long tenantId, String channel, Long sourceId, Long keywordId,
                                            LocalDate startDate, LocalDate endDate, Integer limit);

    /**
     * 执行upsertSnapshot业务操作。
     */
    Map<String, Object> upsertSnapshot(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 查询opportunities列表。
     */
    List<Map<String, Object>> listOpportunities(Long tenantId, String channel, Long sourceId,
                                                String status, String severity, Integer limit);

    /**
     * 执行evaluateOpportunities业务操作。
     */
    Map<String, Object> evaluateOpportunities(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 更新opportunityStatus业务对象。
     */
    Map<String, Object> updateOpportunityStatus(Long tenantId, Long opportunityId, Map<String, Object> payload,
                                                String actor);

    /**
     * 创建opportunityMutation业务对象。
     */
    Map<String, Object> createOpportunityMutation(Long tenantId, Long opportunityId, Map<String, Object> payload,
                                                  String actor);

    /**
     * 查询mutations列表。
     */
    List<Map<String, Object>> listMutations(Long tenantId, Long sourceId, String status, String approvalStatus,
                                            Integer limit);

    /**
     * 执行upsertMutation业务操作。
     */
    Map<String, Object> upsertMutation(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 执行approveMutation业务操作。
     */
    Map<String, Object> approveMutation(Long tenantId, Long mutationId, Map<String, Object> payload, String actor);

    /**
     * 执行executeMutation业务操作。
     */
    Map<String, Object> executeMutation(Long tenantId, Long mutationId, Map<String, Object> payload, String actor);

    /**
     * 查询urlInspections列表。
     */
    List<Map<String, Object>> listUrlInspections(Long tenantId, Long sourceId, String indexedState,
                                                 LocalDate startDate, LocalDate endDate, Integer limit);

    /**
     * 查询syncRuns列表。
     */
    List<Map<String, Object>> listSyncRuns(Long tenantId, Long sourceId, String runType, String status,
                                           Integer limit);

    /**
     * 执行syncSource业务操作。
     */
    Map<String, Object> syncSource(Long tenantId, Long sourceId, Map<String, Object> payload, String actor);

    /**
     * 执行syncDue业务操作。
     */
    Map<String, Object> syncDue(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 查询providerChanges列表。
     */
    List<Map<String, Object>> listProviderChanges(Long tenantId, Long sourceId, Long mutationId, String provider,
                                                  String reconciliationStatus, Integer limit);

    /**
     * 执行reconcileMutation业务操作。
     */
    Map<String, Object> reconcileMutation(Long tenantId, Long mutationId, String actor);

    /**
     * 查询impactWindows列表。
     */
    List<Map<String, Object>> listImpactWindows(Long tenantId, Long opportunityId, Long mutationId, Long sourceId,
                                                String status, String decision, Integer limit);

    /**
     * 执行evaluateDueImpactWindows业务操作。
     */
    Map<String, Object> evaluateDueImpactWindows(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 执行readiness业务操作。
     */
    Map<String, Object> readiness(Long tenantId);

    /**
     * 执行summary业务操作。
     */
    Map<String, Object> summary(Long tenantId, String channel, Long sourceId, Long keywordId,
                                LocalDate startDate, LocalDate endDate);
}
