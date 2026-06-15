package org.chovy.canvas.cdp.api;

import java.util.List;
import java.util.Map;

public interface CdpWarehouseDataPathProbeFacade {

    Map<String, Object> run(Long tenantId, RunCommand command);

    List<Map<String, Object>> recent(Long tenantId, int limit);

    record RunCommand(String probeKey,
                      String eventCode,
                      boolean strict,
                      int verifyAttempts,
                      int verifyDelayMs,
                      String sourceMode) {
    }

}
