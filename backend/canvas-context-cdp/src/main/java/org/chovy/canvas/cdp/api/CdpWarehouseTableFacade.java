package org.chovy.canvas.cdp.api;

import java.util.Map;

public interface CdpWarehouseTableFacade {

    Map<String, Object> listContracts(Long tenantId, String layer, String lifecycleStatus);

    Map<String, Object> upsertContract(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> inspectContract(Long tenantId, String tableKey, String actor, boolean live);

    Map<String, Object> inspectAll(Long tenantId, String actor, boolean live);

    Map<String, Object> planRemediation(Long tenantId, String tableKey, boolean live, String actor);

    Map<String, Object> planAllRemediation(Long tenantId, boolean live, String actor);

    Map<String, Object> scanIncidents(Long tenantId, boolean live, String actor);
}
