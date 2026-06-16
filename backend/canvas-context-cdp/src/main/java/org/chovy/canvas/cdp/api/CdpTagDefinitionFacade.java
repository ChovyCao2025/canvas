package org.chovy.canvas.cdp.api;

import java.util.Map;

/**
 * 定义 CdpTagDefinitionFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpTagDefinitionFacade {

    /**
     * enabled)。
     */
    Map<String, Object> list(Long tenantId, Integer page, Integer size, String tagType, Integer enabled);

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
    Map<String, Object> delete(Long tenantId, Long id, String actor);

    /**
     * enabled)。
     */
    Map<String, Object> listValues(Long tenantId, String tagCode, Integer enabled);

    /**
     * actor)。
     */
    Map<String, Object> createValue(Long tenantId, String tagCode, Map<String, Object> payload, String actor);

    /**
     * actor)。
     */
    Map<String, Object> updateValue(Long tenantId, Long id, Map<String, Object> payload, String actor);

    /**
     * actor)。
     */
    Map<String, Object> deleteValue(Long tenantId, Long id, String actor);
}
