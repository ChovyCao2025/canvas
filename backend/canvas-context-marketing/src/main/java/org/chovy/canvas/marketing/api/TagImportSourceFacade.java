package org.chovy.canvas.marketing.api;

import java.util.Map;

public interface TagImportSourceFacade {

    Map<String, Object> listSources(Long tenantId, Integer enabled);

    Map<String, Object> createSource(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> updateSource(Long tenantId, Long id, Map<String, Object> payload, String actor);

    void deleteSource(Long tenantId, Long id);

    Map<String, Object> runSource(Long tenantId, Long id);
}
