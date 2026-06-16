package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 定义 CdpWarehouseQualityFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpWarehouseQualityFacade {

    /**
     * limit)。
     */
    List<Map<String, Object>> recentChecks(Long tenantId, int limit);

    Map<String, Object> reconcileOds(Long tenantId, LocalDateTime from, LocalDateTime to, Long tolerance,
                                      /**
                                       * operator)。
                                       */
                                      String operator);

    /**
     * operator)。
     */
    Map<String, Object> checkAggregateLag(Long tenantId, LocalDateTime now, Long maxLagMinutes, String operator);
}
