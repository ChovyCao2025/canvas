package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface CdpWarehouseProductionReadinessFacade {

    Map<String, Object> proof(
            Long tenantId,
            LocalDateTime from,
            LocalDateTime to,
            String mode,
            List<String> contractKeys);
}
