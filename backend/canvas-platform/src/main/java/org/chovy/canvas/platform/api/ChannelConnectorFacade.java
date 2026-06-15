package org.chovy.canvas.platform.api;

import java.util.List;
import java.util.Map;

public interface ChannelConnectorFacade {

    List<Map<String, Object>> connectors(Long tenantId);

    List<Map<String, Object>> limits(Long tenantId);

    Map<String, Object> updateMode(Long tenantId, Long connectorId, Map<String, Object> payload, String actor);

    Map<String, Object> healthTest(Long tenantId, Long connectorId);

    Map<String, Object> validateFallback(Long tenantId, Map<String, Object> payload);

    List<Map<String, Object>> fallbackDecisions(Long tenantId);

    List<Map<String, Object>> dedupeRecords(Long tenantId);
}
