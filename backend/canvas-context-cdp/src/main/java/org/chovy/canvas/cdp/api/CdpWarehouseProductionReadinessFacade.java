package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 定义 CdpWarehouseProductionReadinessFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpWarehouseProductionReadinessFacade {

    Map<String, Object> proof(
            Long tenantId,
            LocalDateTime from,
            LocalDateTime to,
            String mode,
            /**
             * contract Keys)。
             */
            List<String> contractKeys);
}
