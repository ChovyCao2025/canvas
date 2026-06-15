package org.chovy.canvas.cdp.api;

import java.util.List;
import java.util.Map;

public interface CdpWarehousePrivacyFacade {

    Map<String, Object> createErasureRequest(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> recordAssetProof(Long tenantId, Long requestId, Map<String, Object> payload, String actor);

    Map<String, Object> executeErasure(Long tenantId, Long requestId, Map<String, Object> payload, String actor);

    Map<String, Object> rebuildAudienceBitmaps(Long tenantId, Long requestId, Map<String, Object> payload, String actor);

    Map<String, Object> runAudienceRebuildAutomation(Long tenantId, Map<String, Object> payload, String actor);

    List<Map<String, Object>> listAudienceRebuildAutomationRuns(Long tenantId, Integer limit);

    Map<String, Object> getAudienceRebuildAutomationRun(Long tenantId, Long runId);

    List<Map<String, Object>> recentErasureRequests(Long tenantId, String status, Integer limit);

    Map<String, Object> getErasureRequest(Long tenantId, Long requestId);

    Map<String, Object> erasureSummary(Long tenantId);

    Map<String, Object> createTombstone(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> createTombstoneFromErasureRequest(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> revokeTombstone(Long tenantId, Long tombstoneId, Map<String, Object> payload, String actor);

    List<Map<String, Object>> listTombstones(Long tenantId, String status, Integer limit);

    Map<String, Object> tombstoneDecision(Long tenantId, String subjectType, String subjectValue);
}
