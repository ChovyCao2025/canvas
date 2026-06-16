package org.chovy.canvas.cdp.api;

import java.util.List;
import java.util.Map;

/**
 * 定义 CdpWarehousePrivacyFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpWarehousePrivacyFacade {

    /**
     * actor)。
     */
    Map<String, Object> createErasureRequest(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * actor)。
     */
    Map<String, Object> recordAssetProof(Long tenantId, Long requestId, Map<String, Object> payload, String actor);

    /**
     * actor)。
     */
    Map<String, Object> executeErasure(Long tenantId, Long requestId, Map<String, Object> payload, String actor);

    /**
     * actor)。
     */
    Map<String, Object> rebuildAudienceBitmaps(Long tenantId, Long requestId, Map<String, Object> payload, String actor);

    /**
     * actor)。
     */
    Map<String, Object> runAudienceRebuildAutomation(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * limit)。
     */
    List<Map<String, Object>> listAudienceRebuildAutomationRuns(Long tenantId, Integer limit);

    /**
     * run Id)。
     */
    Map<String, Object> getAudienceRebuildAutomationRun(Long tenantId, Long runId);

    /**
     * limit)。
     */
    List<Map<String, Object>> recentErasureRequests(Long tenantId, String status, Integer limit);

    /**
     * request Id)。
     */
    Map<String, Object> getErasureRequest(Long tenantId, Long requestId);

    /**
     * tenant Id)。
     */
    Map<String, Object> erasureSummary(Long tenantId);

    /**
     * actor)。
     */
    Map<String, Object> createTombstone(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * actor)。
     */
    Map<String, Object> createTombstoneFromErasureRequest(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * actor)。
     */
    Map<String, Object> revokeTombstone(Long tenantId, Long tombstoneId, Map<String, Object> payload, String actor);

    /**
     * limit)。
     */
    List<Map<String, Object>> listTombstones(Long tenantId, String status, Integer limit);

    /**
     * subject Value)。
     */
    Map<String, Object> tombstoneDecision(Long tenantId, String subjectType, String subjectValue);
}
