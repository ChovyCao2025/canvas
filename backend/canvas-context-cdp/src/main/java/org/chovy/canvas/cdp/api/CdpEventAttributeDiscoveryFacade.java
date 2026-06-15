package org.chovy.canvas.cdp.api;

import java.util.List;

public interface CdpEventAttributeDiscoveryFacade {

    List<DiscoveredAttributeView> listDiscovered(String status);

    record DiscoveredAttributeView(
            Long id,
            String eventCode,
            String attrName,
            String attrType,
            String status,
            String sampleValue,
            String firstSeenAt,
            String lastSeenAt) {
    }
}
