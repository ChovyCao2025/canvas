package org.chovy.canvas.cdp.api;

import java.util.List;
import java.util.Map;

public interface CdpWarehouseRealtimeFacade {

    Map<String, Object> realtimeStatus(Long tenantId);

    Map<String, Object> registerSchema(Long tenantId, Map<String, Object> payload, String actor);

    List<Map<String, Object>> listSchemas(Long tenantId, String pipelineKey, String schemaRole, Integer limit);

    Map<String, Object> latestSchema(Long tenantId, String pipelineKey, String schemaRole);

    List<Map<String, Object>> listPipelineContracts(Long tenantId, String lifecycleStatus);

    Map<String, Object> upsertPipelineContract(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> reportCheckpoint(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> pipelineStatus(Long tenantId, Integer recentLimit);

    Map<String, Object> scanJobIncidents(Long tenantId, String pipelineKey, Long maxHeartbeatAgeSeconds, Integer limit);

    Map<String, Object> heartbeat(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> jobStatus(Long tenantId, String pipelineKey, Long maxHeartbeatAgeSeconds, Integer limit);

    Map<String, Object> requestAction(Long tenantId, Map<String, Object> payload, String actor);

    List<Map<String, Object>> pendingActions(Long tenantId, String pipelineKey, String jobKey, Integer limit);

    Map<String, Object> acknowledgeAction(Long tenantId, Long actionId);

    Map<String, Object> completeAction(Long tenantId, Long actionId, String status, String resultMessage);

    Map<String, Object> scanPipelineIncidents(Long tenantId, Integer recentLimit);

    Map<String, Object> upsertProbeTarget(Long tenantId, Map<String, Object> payload, String actor);

    List<Map<String, Object>> listProbeTargets(Long tenantId, Boolean includeDisabled, Integer limit);

    Map<String, Object> setProbeTargetEnabled(Long tenantId, Long targetId, Boolean enabled);

    Map<String, Object> scanProbeTargets(Long tenantId, Long targetId, Integer limit);
}
