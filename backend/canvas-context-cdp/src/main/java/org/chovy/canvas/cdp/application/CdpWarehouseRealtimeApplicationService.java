package org.chovy.canvas.cdp.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseRealtimeFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseRealtimeCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CdpWarehouseRealtimeApplicationService implements CdpWarehouseRealtimeFacade {

    private final CdpWarehouseRealtimeCatalog catalog = new CdpWarehouseRealtimeCatalog();

    @Override
    public Map<String, Object> realtimeStatus(Long tenantId) {
        return catalog.realtimeStatus(safeTenantId(tenantId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> registerSchema(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.registerSchema(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> listSchemas(Long tenantId, String pipelineKey, String schemaRole, Integer limit) {
        return catalog.listSchemas(safeTenantId(tenantId), pipelineKey, schemaRole, normalizedLimit(limit));
    }

    @Override
    public Map<String, Object> latestSchema(Long tenantId, String pipelineKey, String schemaRole) {
        return catalog.latestSchema(safeTenantId(tenantId), pipelineKey, schemaRole);
    }

    @Override
    public List<Map<String, Object>> listPipelineContracts(Long tenantId, String lifecycleStatus) {
        return catalog.listPipelineContracts(safeTenantId(tenantId), lifecycleStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertPipelineContract(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertPipelineContract(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reportCheckpoint(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.reportCheckpoint(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    @Override
    public Map<String, Object> pipelineStatus(Long tenantId, Integer recentLimit) {
        return catalog.pipelineStatus(safeTenantId(tenantId), normalizedLimit(recentLimit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> scanJobIncidents(Long tenantId, String pipelineKey, Long maxHeartbeatAgeSeconds,
                                                Integer limit) {
        return catalog.scanJobIncidents(safeTenantId(tenantId), pipelineKey,
                maxHeartbeatAgeSeconds == null ? 300L : maxHeartbeatAgeSeconds, normalizedLimit(limit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> heartbeat(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.heartbeat(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    @Override
    public Map<String, Object> jobStatus(Long tenantId, String pipelineKey, Long maxHeartbeatAgeSeconds,
                                         Integer limit) {
        return catalog.jobStatus(safeTenantId(tenantId), pipelineKey,
                maxHeartbeatAgeSeconds == null ? 300L : maxHeartbeatAgeSeconds, normalizedLimit(limit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> requestAction(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.requestAction(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> pendingActions(Long tenantId, String pipelineKey, String jobKey, Integer limit) {
        return catalog.pendingActions(safeTenantId(tenantId), pipelineKey, jobKey, normalizedLimit(limit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> acknowledgeAction(Long tenantId, Long actionId) {
        return catalog.acknowledgeAction(safeTenantId(tenantId), actionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> completeAction(Long tenantId, Long actionId, String status, String resultMessage) {
        return catalog.completeAction(safeTenantId(tenantId), actionId, status, resultMessage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> scanPipelineIncidents(Long tenantId, Integer recentLimit) {
        return catalog.scanPipelineIncidents(safeTenantId(tenantId), normalizedLimit(recentLimit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertProbeTarget(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertProbeTarget(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> listProbeTargets(Long tenantId, Boolean includeDisabled, Integer limit) {
        return catalog.listProbeTargets(safeTenantId(tenantId), Boolean.TRUE.equals(includeDisabled),
                normalizedLimit(limit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> setProbeTargetEnabled(Long tenantId, Long targetId, Boolean enabled) {
        return catalog.setProbeTargetEnabled(safeTenantId(tenantId), targetId, Boolean.TRUE.equals(enabled));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> scanProbeTargets(Long tenantId, Long targetId, Integer limit) {
        return catalog.scanProbeTargets(safeTenantId(tenantId), targetId, normalizedLimit(limit));
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
        return limit == null ? 50 : Math.max(1, Math.min(limit, 100));
    }
}
