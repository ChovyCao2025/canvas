package org.chovy.canvas.cdp.api;

import java.util.List;
import java.util.Map;

/**
 * 定义 CdpWarehouseIncidentFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpWarehouseIncidentFacade {

    /**
     * limit)。
     */
    List<Map<String, Object>> listIncidents(Long tenantId, String status, int limit);

    /**
     * 执行 acknowledge 对应的 CDP 业务操作。
     */
    boolean acknowledge(Long tenantId, Long incidentId, String operator);

    /**
     * 执行 resolve 对应的 CDP 业务操作。
     */
    boolean resolve(Long tenantId, Long incidentId, String operator);
}
