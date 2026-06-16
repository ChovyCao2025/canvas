package org.chovy.canvas.cdp.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseRealtimeFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseRealtimeCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排 CdpWarehouseRealtime 的应用服务流程。
 */
@Service
public class CdpWarehouseRealtimeApplicationService implements CdpWarehouseRealtimeFacade {

    /**
     * 执行 CdpWarehouseRealtimeCatalog 对应的 CDP 业务操作。
     */
    private final CdpWarehouseRealtimeCatalog catalog = new CdpWarehouseRealtimeCatalog();

    /**
     * 执行 realtimeStatus 对应的 CDP 业务操作。
     */
    @Override
    public Map<String, Object> realtimeStatus(Long tenantId) {
        return catalog.realtimeStatus(safeTenantId(tenantId));
    }

    /**
     * 执行 registerSchema 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> registerSchema(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.registerSchema(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 查询Schemas列表。
     */
    @Override
    public List<Map<String, Object>> listSchemas(Long tenantId, String pipelineKey, String schemaRole, Integer limit) {
        return catalog.listSchemas(safeTenantId(tenantId), pipelineKey, schemaRole, normalizedLimit(limit));
    }

    /**
     * 执行 latestSchema 对应的 CDP 业务操作。
     */
    @Override
    public Map<String, Object> latestSchema(Long tenantId, String pipelineKey, String schemaRole) {
        return catalog.latestSchema(safeTenantId(tenantId), pipelineKey, schemaRole);
    }

    /**
     * 查询Pipeline Contracts列表。
     */
    @Override
    public List<Map<String, Object>> listPipelineContracts(Long tenantId, String lifecycleStatus) {
        return catalog.listPipelineContracts(safeTenantId(tenantId), lifecycleStatus);
    }

    /**
     * 执行 upsertPipelineContract 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertPipelineContract(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertPipelineContract(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 执行 reportCheckpoint 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reportCheckpoint(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.reportCheckpoint(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 执行 pipelineStatus 对应的 CDP 业务操作。
     */
    @Override
    public Map<String, Object> pipelineStatus(Long tenantId, Integer recentLimit) {
        return catalog.pipelineStatus(safeTenantId(tenantId), normalizedLimit(recentLimit));
    }

    /**
     * 执行 scanJobIncidents 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> scanJobIncidents(Long tenantId, String pipelineKey, Long maxHeartbeatAgeSeconds,
                                                Integer limit) {
        return catalog.scanJobIncidents(safeTenantId(tenantId), pipelineKey,
                maxHeartbeatAgeSeconds == null ? 300L : maxHeartbeatAgeSeconds, normalizedLimit(limit));
    }

    /**
     * 执行 heartbeat 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> heartbeat(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.heartbeat(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 执行 jobStatus 对应的 CDP 业务操作。
     */
    @Override
    public Map<String, Object> jobStatus(Long tenantId, String pipelineKey, Long maxHeartbeatAgeSeconds,
                                         Integer limit) {
        return catalog.jobStatus(safeTenantId(tenantId), pipelineKey,
                maxHeartbeatAgeSeconds == null ? 300L : maxHeartbeatAgeSeconds, normalizedLimit(limit));
    }

    /**
     * 执行 requestAction 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> requestAction(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.requestAction(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 执行 pendingActions 对应的 CDP 业务操作。
     */
    @Override
    public List<Map<String, Object>> pendingActions(Long tenantId, String pipelineKey, String jobKey, Integer limit) {
        return catalog.pendingActions(safeTenantId(tenantId), pipelineKey, jobKey, normalizedLimit(limit));
    }

    /**
     * 执行 acknowledgeAction 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> acknowledgeAction(Long tenantId, Long actionId) {
        return catalog.acknowledgeAction(safeTenantId(tenantId), actionId);
    }

    /**
     * 执行 completeAction 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> completeAction(Long tenantId, Long actionId, String status, String resultMessage) {
        return catalog.completeAction(safeTenantId(tenantId), actionId, status, resultMessage);
    }

    /**
     * 执行 scanPipelineIncidents 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> scanPipelineIncidents(Long tenantId, Integer recentLimit) {
        return catalog.scanPipelineIncidents(safeTenantId(tenantId), normalizedLimit(recentLimit));
    }

    /**
     * 执行 upsertProbeTarget 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertProbeTarget(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertProbeTarget(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 查询Probe Targets列表。
     */
    @Override
    public List<Map<String, Object>> listProbeTargets(Long tenantId, Boolean includeDisabled, Integer limit) {
        return catalog.listProbeTargets(safeTenantId(tenantId), Boolean.TRUE.equals(includeDisabled),
                normalizedLimit(limit));
    }

    /**
     * 设置probe Target Enabled。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> setProbeTargetEnabled(Long tenantId, Long targetId, Boolean enabled) {
        return catalog.setProbeTargetEnabled(safeTenantId(tenantId), targetId, Boolean.TRUE.equals(enabled));
    }

    /**
     * 执行 scanProbeTargets 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> scanProbeTargets(Long tenantId, Long targetId, Integer limit) {
        return catalog.scanProbeTargets(safeTenantId(tenantId), targetId, normalizedLimit(limit));
    }

    /**
     * 返回安全的Tenant Id。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 返回安全的Payload。
     */
    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    /**
     * 执行 actorOrDefault 对应的 CDP 业务操作。
     */
    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    /**
     * 归一化d Limit。
     */
    private static int normalizedLimit(Integer limit) {
        return limit == null ? 50 : Math.max(1, Math.min(limit, 100));
    }
}
