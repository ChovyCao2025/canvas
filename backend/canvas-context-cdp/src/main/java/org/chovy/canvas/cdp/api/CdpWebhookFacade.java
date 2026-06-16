package org.chovy.canvas.cdp.api;

import java.util.Map;

/**
 * 定义 CdpWebhookFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpWebhookFacade {

    /**
     * tenant Id)。
     */
    Map<String, Object> list(Long tenantId);

    /**
     * actor)。
     */
    Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * actor)。
     */
    Map<String, Object> update(Long tenantId, Long id, Map<String, Object> payload, String actor);

    /**
     * actor)。
     */
    Map<String, Object> pause(Long tenantId, Long id, String actor);

    /**
     * actor)。
     */
    Map<String, Object> resume(Long tenantId, Long id, String actor);

    /**
     * actor)。
     */
    Map<String, Object> disable(Long tenantId, Long id, String actor);

    /**
     * actor)。
     */
    Map<String, Object> rotateSecret(Long tenantId, Long id, String actor);

    /**
     * actor)。
     */
    Map<String, Object> testDelivery(Long tenantId, Long id, String actor);

    /**
     * 执行 deliveries 对应的 CDP 业务操作。
     */
    default Map<String, Object> deliveries(Long tenantId, Long id) {
        return deliveries(tenantId, id, null);
    }

    /**
     * limit)。
     */
    Map<String, Object> deliveries(Long tenantId, Long id, Integer limit);
}
