package org.chovy.canvas.cdp.domain;

import java.util.Map;

/**
 * 定义 CdpEventAttributeDiscoveryPort 的协作契约。
 */
public interface CdpEventAttributeDiscoveryPort {

    /**
     * 执行 discover 对应的 CDP 业务操作。
     */
    void discover(Long tenantId, String eventCode, Map<String, Object> properties);

    /**
     * 执行 noop 对应的 CDP 业务操作。
     */
    static CdpEventAttributeDiscoveryPort noop() {
        return (tenantId, eventCode, properties) -> {
        };
    }
}
