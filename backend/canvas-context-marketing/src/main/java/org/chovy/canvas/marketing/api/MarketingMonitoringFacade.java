package org.chovy.canvas.marketing.api;

import java.util.List;
import java.util.Map;

/**
 * 定义MarketingMonitoringFacade的营销上下文访问契约。
 */
public interface MarketingMonitoringFacade {

    /**
     * 执行execute业务操作。
     */
    Map<String, Object> execute(Long tenantId, String operation, Map<String, Object> payload, String actor);

    /**
     * 查询列表。
     */
    List<Map<String, Object>> list(Long tenantId, String operation, Map<String, Object> criteria, Integer limit);
}
