package org.chovy.canvas.marketing.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface SearchMarketingFacade {

    List<Map<String, Object>> listSources(Long tenantId, String provider, String channel, Boolean enabled, Integer limit);

    Map<String, Object> upsertSource(Long tenantId, Map<String, Object> payload, String actor);

    List<Map<String, Object>> listKeywords(Long tenantId, String channel, String status, Integer limit);

    Map<String, Object> upsertKeyword(Long tenantId, Map<String, Object> payload, String actor);

    List<Map<String, Object>> listSnapshots(Long tenantId, String channel, Long sourceId, Long keywordId,
                                            LocalDate startDate, LocalDate endDate, Integer limit);

    Map<String, Object> upsertSnapshot(Long tenantId, Map<String, Object> payload, String actor);

    List<Map<String, Object>> listOpportunities(Long tenantId, String channel, Long sourceId,
                                                String status, String severity, Integer limit);

    Map<String, Object> evaluateOpportunities(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> updateOpportunityStatus(Long tenantId, Long opportunityId, Map<String, Object> payload,
                                                String actor);

    Map<String, Object> createOpportunityMutation(Long tenantId, Long opportunityId, Map<String, Object> payload,
                                                  String actor);

    List<Map<String, Object>> listMutations(Long tenantId, Long sourceId, String status, String approvalStatus,
                                            Integer limit);

    Map<String, Object> upsertMutation(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> approveMutation(Long tenantId, Long mutationId, Map<String, Object> payload, String actor);

    Map<String, Object> executeMutation(Long tenantId, Long mutationId, Map<String, Object> payload, String actor);

    List<Map<String, Object>> listUrlInspections(Long tenantId, Long sourceId, String indexedState,
                                                 LocalDate startDate, LocalDate endDate, Integer limit);

    List<Map<String, Object>> listSyncRuns(Long tenantId, Long sourceId, String runType, String status,
                                           Integer limit);

    Map<String, Object> syncSource(Long tenantId, Long sourceId, Map<String, Object> payload, String actor);

    Map<String, Object> syncDue(Long tenantId, Map<String, Object> payload, String actor);

    List<Map<String, Object>> listProviderChanges(Long tenantId, Long sourceId, Long mutationId, String provider,
                                                  String reconciliationStatus, Integer limit);

    Map<String, Object> reconcileMutation(Long tenantId, Long mutationId, String actor);

    List<Map<String, Object>> listImpactWindows(Long tenantId, Long opportunityId, Long mutationId, Long sourceId,
                                                String status, String decision, Integer limit);

    Map<String, Object> evaluateDueImpactWindows(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> readiness(Long tenantId);

    Map<String, Object> summary(Long tenantId, String channel, Long sourceId, Long keywordId,
                                LocalDate startDate, LocalDate endDate);
}
