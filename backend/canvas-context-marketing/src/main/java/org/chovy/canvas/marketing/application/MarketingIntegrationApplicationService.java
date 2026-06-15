package org.chovy.canvas.marketing.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MarketingIntegrationFacade;
import org.chovy.canvas.marketing.domain.MarketingIntegrationCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketingIntegrationApplicationService implements MarketingIntegrationFacade {

    private final MarketingIntegrationCatalog catalog = new MarketingIntegrationCatalog();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertContract(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertContract(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> listContracts(Long tenantId, String status, String providerFamily, Integer limit) {
        return catalog.listContracts(safeTenantId(tenantId), status, providerFamily, normalizedLimit(limit));
    }

    @Override
    public List<Map<String, Object>> listContractAuditEvents(Long tenantId, Long contractId, Integer limit) {
        return catalog.listContractAuditEvents(safeTenantId(tenantId), requiredId(contractId, "contractId"),
                normalizedLimit(limit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> archiveContract(Long tenantId, Long contractId, String actor) {
        return catalog.archiveContract(safeTenantId(tenantId), requiredId(contractId, "contractId"),
                actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> recordProbeRun(Long tenantId, Long contractId, Map<String, Object> payload,
                                              String actor) {
        return catalog.recordProbeRun(safeTenantId(tenantId), requiredId(contractId, "contractId"),
                safePayload(payload), actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> listProbeRuns(Long tenantId, String status, String providerFamily, Integer limit) {
        return catalog.listProbeRuns(safeTenantId(tenantId), status, providerFamily, normalizedLimit(limit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> scanProbeRuns(Long tenantId, Integer limit, String actor) {
        return catalog.scanProbeRuns(safeTenantId(tenantId), normalizedLimit(limit), actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> listContractSloEvaluations(Long tenantId, Integer limit) {
        return catalog.listContractSloEvaluations(safeTenantId(tenantId), normalizedLimit(limit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> recordProbe(Long tenantId, Long contractId, Map<String, Object> payload, String actor) {
        return catalog.recordProbe(safeTenantId(tenantId), requiredId(contractId, "contractId"), safePayload(payload),
                actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> listContractProbes(Long tenantId, Long contractId, Integer limit) {
        return catalog.listContractProbes(safeTenantId(tenantId), requiredId(contractId, "contractId"),
                normalizedLimit(limit));
    }

    @Override
    public List<Map<String, Object>> listRecentProbes(Long tenantId, String status, Integer limit) {
        return catalog.listRecentProbes(safeTenantId(tenantId), status, normalizedLimit(limit));
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
