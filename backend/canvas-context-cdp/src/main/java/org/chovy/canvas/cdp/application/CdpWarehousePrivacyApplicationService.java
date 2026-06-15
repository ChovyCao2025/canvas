package org.chovy.canvas.cdp.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehousePrivacyFacade;
import org.chovy.canvas.cdp.domain.CdpWarehousePrivacyCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CdpWarehousePrivacyApplicationService implements CdpWarehousePrivacyFacade {

    private final CdpWarehousePrivacyCatalog catalog = new CdpWarehousePrivacyCatalog();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createErasureRequest(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createErasureRequest(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> recordAssetProof(Long tenantId, Long requestId, Map<String, Object> payload, String actor) {
        return catalog.recordAssetProof(safeTenantId(tenantId), requiredId(requestId, "requestId"), safePayload(payload),
                actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> executeErasure(Long tenantId, Long requestId, Map<String, Object> payload, String actor) {
        return catalog.executeErasure(safeTenantId(tenantId), requiredId(requestId, "requestId"), safePayload(payload),
                actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> rebuildAudienceBitmaps(Long tenantId, Long requestId, Map<String, Object> payload,
                                                      String actor) {
        return catalog.rebuildAudienceBitmaps(safeTenantId(tenantId), requiredId(requestId, "requestId"),
                safePayload(payload), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> runAudienceRebuildAutomation(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.runAudienceRebuildAutomation(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> listAudienceRebuildAutomationRuns(Long tenantId, Integer limit) {
        return catalog.listAudienceRebuildAutomationRuns(safeTenantId(tenantId), normalizedLimit(limit));
    }

    @Override
    public Map<String, Object> getAudienceRebuildAutomationRun(Long tenantId, Long runId) {
        return catalog.getAudienceRebuildAutomationRun(safeTenantId(tenantId), requiredId(runId, "runId"));
    }

    @Override
    public List<Map<String, Object>> recentErasureRequests(Long tenantId, String status, Integer limit) {
        return catalog.recentErasureRequests(safeTenantId(tenantId), status, normalizedLimit(limit));
    }

    @Override
    public Map<String, Object> getErasureRequest(Long tenantId, Long requestId) {
        return catalog.getErasureRequest(safeTenantId(tenantId), requiredId(requestId, "requestId"));
    }

    @Override
    public Map<String, Object> erasureSummary(Long tenantId) {
        return catalog.erasureSummary(safeTenantId(tenantId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createTombstone(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createTombstone(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createTombstoneFromErasureRequest(Long tenantId, Map<String, Object> payload,
                                                                 String actor) {
        return catalog.createTombstoneFromErasureRequest(safeTenantId(tenantId), safePayload(payload),
                actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> revokeTombstone(Long tenantId, Long tombstoneId, Map<String, Object> payload,
                                               String actor) {
        return catalog.revokeTombstone(safeTenantId(tenantId), requiredId(tombstoneId, "tombstoneId"),
                safePayload(payload), actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> listTombstones(Long tenantId, String status, Integer limit) {
        return catalog.listTombstones(safeTenantId(tenantId), status, normalizedLimit(limit));
    }

    @Override
    public Map<String, Object> tombstoneDecision(Long tenantId, String subjectType, String subjectValue) {
        return catalog.tombstoneDecision(safeTenantId(tenantId), subjectType, subjectValue);
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

    private static Long requiredId(Long id, String name) {
        if (id == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return id;
    }
}
