package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;
import java.util.Map;

public interface CdpWarehouseOperationsFacade {

    Map<String, Object> status(Long tenantId, int limit);

    Map<String, Object> triggerBackfill(Long tenantId, Long lastId, int limit, String operator);

    Map<String, Object> triggerAggregation(Long tenantId, LocalDateTime from, LocalDateTime to, String operator);
}
