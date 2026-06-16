package org.chovy.canvas.marketing.api;

import java.util.List;
import java.util.Map;

/**
 * 定义MarketingIntegrationFacade的营销上下文访问契约。
 */
public interface MarketingIntegrationFacade {

    /**
     * 执行upsertContract业务操作。
     */
    Map<String, Object> upsertContract(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 查询contracts列表。
     */
    List<Map<String, Object>> listContracts(Long tenantId, String status, String providerFamily, Integer limit);

    /**
     * 查询contractAuditEvents列表。
     */
    List<Map<String, Object>> listContractAuditEvents(Long tenantId, Long contractId, Integer limit);

    /**
     * 执行archiveContract业务操作。
     */
    Map<String, Object> archiveContract(Long tenantId, Long contractId, String actor);

    /**
     * 执行recordProbeRun业务操作。
     */
    Map<String, Object> recordProbeRun(Long tenantId, Long contractId, Map<String, Object> payload, String actor);

    /**
     * 查询probeRuns列表。
     */
    List<Map<String, Object>> listProbeRuns(Long tenantId, String status, String providerFamily, Integer limit);

    /**
     * 执行scanProbeRuns业务操作。
     */
    Map<String, Object> scanProbeRuns(Long tenantId, Integer limit, String actor);

    /**
     * 查询contractSloEvaluations列表。
     */
    List<Map<String, Object>> listContractSloEvaluations(Long tenantId, Integer limit);

    /**
     * 执行recordProbe业务操作。
     */
    Map<String, Object> recordProbe(Long tenantId, Long contractId, Map<String, Object> payload, String actor);

    /**
     * 查询contractProbes列表。
     */
    List<Map<String, Object>> listContractProbes(Long tenantId, Long contractId, Integer limit);

    /**
     * 查询recentProbes列表。
     */
    List<Map<String, Object>> listRecentProbes(Long tenantId, String status, Integer limit);
}
