package org.chovy.canvas.cdp.api;

import java.util.Map;

/**
 * 定义 CdpWarehouseReadinessFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpWarehouseReadinessFacade {

    /**
     * 执行 readiness 对应的 CDP 业务操作。
     */
    CdpWarehouseReadinessView readiness(Long tenantId);

    /**
     * tenant Id)。
     */
    Map<String, Object> scanIncidents(Long tenantId);
}
