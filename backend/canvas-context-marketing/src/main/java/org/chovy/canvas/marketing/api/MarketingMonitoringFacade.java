package org.chovy.canvas.marketing.api;

import java.util.List;
import java.util.Map;

public interface MarketingMonitoringFacade {

    Map<String, Object> execute(Long tenantId, String operation, Map<String, Object> payload, String actor);

    List<Map<String, Object>> list(Long tenantId, String operation, Map<String, Object> criteria, Integer limit);
}
