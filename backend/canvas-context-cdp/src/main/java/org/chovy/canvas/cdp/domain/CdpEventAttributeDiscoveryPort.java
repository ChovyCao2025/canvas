package org.chovy.canvas.cdp.domain;

import java.util.Map;

public interface CdpEventAttributeDiscoveryPort {

    void discover(Long tenantId, String eventCode, Map<String, Object> properties);

    static CdpEventAttributeDiscoveryPort noop() {
        return (tenantId, eventCode, properties) -> {
        };
    }
}
