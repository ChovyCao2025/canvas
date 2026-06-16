package org.chovy.canvas.cdp.api;

import java.util.Map;

/**
 * 定义 CdpComputedTagFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpComputedTagFacade {

    /**
     * tenant Id)。
     */
    Map<String, Object> list(Long tenantId);

    /**
     * actor)。
     */
    Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * tag Code)。
     */
    Map<String, Object> preview(Long tenantId, String tagCode);

    /**
     * actor)。
     */
    Map<String, Object> activate(Long tenantId, String tagCode, String actor);

    /**
     * actor)。
     */
    Map<String, Object> pause(Long tenantId, String tagCode, String actor);

    /**
     * actor)。
     */
    Map<String, Object> runNow(Long tenantId, String tagCode, String actor);

    /**
     * limit)。
     */
    Map<String, Object> listRuns(Long tenantId, String tagCode, Integer limit);

    /**
     * tag Code)。
     */
    Map<String, Object> lineage(Long tenantId, String tagCode);

    /**
     * actor)。
     */
    Map<String, Object> impactCheck(Long tenantId, String tagCode, Map<String, Object> payload, String actor);
}
