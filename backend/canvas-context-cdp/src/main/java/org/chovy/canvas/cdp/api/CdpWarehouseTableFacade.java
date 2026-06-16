package org.chovy.canvas.cdp.api;

import java.util.Map;

/**
 * 定义 CdpWarehouseTableFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpWarehouseTableFacade {

    /**
     * lifecycle Status)。
     */
    Map<String, Object> listContracts(Long tenantId, String layer, String lifecycleStatus);

    /**
     * actor)。
     */
    Map<String, Object> upsertContract(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * live)。
     */
    Map<String, Object> inspectContract(Long tenantId, String tableKey, String actor, boolean live);

    /**
     * live)。
     */
    Map<String, Object> inspectAll(Long tenantId, String actor, boolean live);

    /**
     * actor)。
     */
    Map<String, Object> planRemediation(Long tenantId, String tableKey, boolean live, String actor);

    /**
     * actor)。
     */
    Map<String, Object> planAllRemediation(Long tenantId, boolean live, String actor);

    /**
     * actor)。
     */
    Map<String, Object> scanIncidents(Long tenantId, boolean live, String actor);
}
