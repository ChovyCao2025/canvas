package org.chovy.canvas.marketing.api;

import java.util.List;
import java.util.Map;

public interface MarketingIntegrationFacade {

    Map<String, Object> upsertContract(Long tenantId, Map<String, Object> payload, String actor);

    List<Map<String, Object>> listContracts(Long tenantId, String status, String providerFamily, Integer limit);

    List<Map<String, Object>> listContractAuditEvents(Long tenantId, Long contractId, Integer limit);

    Map<String, Object> archiveContract(Long tenantId, Long contractId, String actor);

    Map<String, Object> recordProbeRun(Long tenantId, Long contractId, Map<String, Object> payload, String actor);

    List<Map<String, Object>> listProbeRuns(Long tenantId, String status, String providerFamily, Integer limit);

    Map<String, Object> scanProbeRuns(Long tenantId, Integer limit, String actor);

    List<Map<String, Object>> listContractSloEvaluations(Long tenantId, Integer limit);

    Map<String, Object> recordProbe(Long tenantId, Long contractId, Map<String, Object> payload, String actor);

    List<Map<String, Object>> listContractProbes(Long tenantId, Long contractId, Integer limit);

    List<Map<String, Object>> listRecentProbes(Long tenantId, String status, Integer limit);
}
