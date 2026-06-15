package org.chovy.canvas.cdp.api;

import java.util.List;
import java.util.Map;

public interface CdpWarehouseIncidentFacade {

    List<Map<String, Object>> listIncidents(Long tenantId, String status, int limit);

    boolean acknowledge(Long tenantId, Long incidentId, String operator);

    boolean resolve(Long tenantId, Long incidentId, String operator);
}
