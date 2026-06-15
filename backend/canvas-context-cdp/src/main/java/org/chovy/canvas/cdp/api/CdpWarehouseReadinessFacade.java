package org.chovy.canvas.cdp.api;

import java.util.Map;

public interface CdpWarehouseReadinessFacade {

    CdpWarehouseReadinessView readiness(Long tenantId);

    Map<String, Object> scanIncidents(Long tenantId);
}
