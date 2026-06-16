package org.chovy.canvas.cdp.api;

import java.util.Map;

/**
 * 定义 CdpComputedProfileFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpComputedProfileFacade {

    /**
     * tenant Id)。
     */
    Map<String, Object> list(Long tenantId);

    /**
     * actor)。
     */
    Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * id)。
     */
    Map<String, Object> preview(Long tenantId, Long id);

    /**
     * actor)。
     */
    Map<String, Object> activate(Long tenantId, Long id, String actor);

    /**
     * actor)。
     */
    Map<String, Object> pause(Long tenantId, Long id, String actor);

    /**
     * actor)。
     */
    Map<String, Object> runNow(Long tenantId, Long id, String actor);

    /**
     * limit)。
     */
    Map<String, Object> listRuns(Long tenantId, Long id, Integer limit);

    /**
     * limit)。
     */
    Map<String, Object> listChanges(Long tenantId, Long id, String userId, Integer limit);
}
