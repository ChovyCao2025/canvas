package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface CdpWarehouseQualityFacade {

    List<Map<String, Object>> recentChecks(Long tenantId, int limit);

    Map<String, Object> reconcileOds(Long tenantId, LocalDateTime from, LocalDateTime to, Long tolerance,
                                      String operator);

    Map<String, Object> checkAggregateLag(Long tenantId, LocalDateTime now, Long maxLagMinutes, String operator);
}
