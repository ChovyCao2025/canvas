package org.chovy.canvas.cdp.api;

import java.util.Map;

public interface CdpTagDefinitionFacade {

    Map<String, Object> list(Long tenantId, Integer page, Integer size, String tagType, Integer enabled);

    Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> update(Long tenantId, Long id, Map<String, Object> payload, String actor);

    Map<String, Object> delete(Long tenantId, Long id, String actor);

    Map<String, Object> listValues(Long tenantId, String tagCode, Integer enabled);

    Map<String, Object> createValue(Long tenantId, String tagCode, Map<String, Object> payload, String actor);

    Map<String, Object> updateValue(Long tenantId, Long id, Map<String, Object> payload, String actor);

    Map<String, Object> deleteValue(Long tenantId, Long id, String actor);
}
