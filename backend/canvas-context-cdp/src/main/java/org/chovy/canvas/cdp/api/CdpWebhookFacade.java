package org.chovy.canvas.cdp.api;

import java.util.Map;

public interface CdpWebhookFacade {

    Map<String, Object> list(Long tenantId);

    Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> update(Long tenantId, Long id, Map<String, Object> payload, String actor);

    Map<String, Object> pause(Long tenantId, Long id, String actor);

    Map<String, Object> resume(Long tenantId, Long id, String actor);

    Map<String, Object> disable(Long tenantId, Long id, String actor);

    Map<String, Object> rotateSecret(Long tenantId, Long id, String actor);

    Map<String, Object> testDelivery(Long tenantId, Long id, String actor);

    default Map<String, Object> deliveries(Long tenantId, Long id) {
        return deliveries(tenantId, id, null);
    }

    Map<String, Object> deliveries(Long tenantId, Long id, Integer limit);
}
