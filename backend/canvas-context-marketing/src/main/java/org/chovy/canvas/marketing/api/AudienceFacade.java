package org.chovy.canvas.marketing.api;

import java.util.List;
import java.util.Map;

public interface AudienceFacade {

    Map<String, Object> list(Long tenantId, Integer page, Integer size);

    List<Map<String, Object>> sourceFields(String dataSourceType);

    Map<String, Object> preview(Long tenantId, Map<String, Object> payload);

    Map<String, Object> get(Long tenantId, Long id);

    List<Map<String, Object>> ready(Long tenantId);

    Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> update(Long tenantId, Long id, Map<String, Object> payload, String actor);

    Map<String, Object> delete(Long tenantId, Long id);

    Map<String, Object> compute(Long tenantId, Long id, Map<String, Object> payload, String actor);

    Map<String, Object> stat(Long tenantId, Long id);
}
