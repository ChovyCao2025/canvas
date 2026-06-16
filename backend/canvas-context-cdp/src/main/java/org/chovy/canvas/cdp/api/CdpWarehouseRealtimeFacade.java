package org.chovy.canvas.cdp.api;

import java.util.List;
import java.util.Map;

/**
 * 定义 CdpWarehouseRealtimeFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpWarehouseRealtimeFacade {

    /**
     * tenant Id)。
     */
    Map<String, Object> realtimeStatus(Long tenantId);

    /**
     * actor)。
     */
    Map<String, Object> registerSchema(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * limit)。
     */
    List<Map<String, Object>> listSchemas(Long tenantId, String pipelineKey, String schemaRole, Integer limit);

    /**
     * schema Role)。
     */
    Map<String, Object> latestSchema(Long tenantId, String pipelineKey, String schemaRole);

    /**
     * lifecycle Status)。
     */
    List<Map<String, Object>> listPipelineContracts(Long tenantId, String lifecycleStatus);

    /**
     * actor)。
     */
    Map<String, Object> upsertPipelineContract(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * actor)。
     */
    Map<String, Object> reportCheckpoint(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * recent Limit)。
     */
    Map<String, Object> pipelineStatus(Long tenantId, Integer recentLimit);

    /**
     * limit)。
     */
    Map<String, Object> scanJobIncidents(Long tenantId, String pipelineKey, Long maxHeartbeatAgeSeconds, Integer limit);

    /**
     * actor)。
     */
    Map<String, Object> heartbeat(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * limit)。
     */
    Map<String, Object> jobStatus(Long tenantId, String pipelineKey, Long maxHeartbeatAgeSeconds, Integer limit);

    /**
     * actor)。
     */
    Map<String, Object> requestAction(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * limit)。
     */
    List<Map<String, Object>> pendingActions(Long tenantId, String pipelineKey, String jobKey, Integer limit);

    /**
     * action Id)。
     */
    Map<String, Object> acknowledgeAction(Long tenantId, Long actionId);

    /**
     * result Message)。
     */
    Map<String, Object> completeAction(Long tenantId, Long actionId, String status, String resultMessage);

    /**
     * recent Limit)。
     */
    Map<String, Object> scanPipelineIncidents(Long tenantId, Integer recentLimit);

    /**
     * actor)。
     */
    Map<String, Object> upsertProbeTarget(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * limit)。
     */
    List<Map<String, Object>> listProbeTargets(Long tenantId, Boolean includeDisabled, Integer limit);

    /**
     * enabled)。
     */
    Map<String, Object> setProbeTargetEnabled(Long tenantId, Long targetId, Boolean enabled);

    /**
     * limit)。
     */
    Map<String, Object> scanProbeTargets(Long tenantId, Long targetId, Integer limit);
}
