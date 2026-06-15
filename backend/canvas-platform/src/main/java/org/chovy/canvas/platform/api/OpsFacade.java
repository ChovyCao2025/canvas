package org.chovy.canvas.platform.api;

import java.util.List;
import java.util.Map;

public interface OpsFacade {

    Map<String, Object> invalidateCache(Long tenantId, Long canvasId, String actor);

    Map<String, Object> rebuildRuntimeState(Long tenantId, String actor);

    Map<String, Object> runtimeStatus(Long tenantId, String role, String actor);

    List<Map<String, Object>> auditEvents(Long tenantId, Integer limit);

    Map<String, Object> emergencyAction(Long tenantId, Long canvasId, String action, Map<String, Object> payload,
                                        String role, String actor);
}
