package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 定义 CdpWarehouseOperationsFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpWarehouseOperationsFacade {

    /**
     * limit)。
     */
    Map<String, Object> status(Long tenantId, int limit);

    /**
     * operator)。
     */
    Map<String, Object> triggerBackfill(Long tenantId, Long lastId, int limit, String operator);

    /**
     * operator)。
     */
    Map<String, Object> triggerAggregation(Long tenantId, LocalDateTime from, LocalDateTime to, String operator);
}
