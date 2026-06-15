package org.chovy.canvas.platform.api;

import java.util.List;
import java.util.Map;

public interface TestUserFacade {

    List<Map<String, Object>> listSets(Long tenantId);

    Map<String, Object> createSet(Long tenantId, Map<String, Object> payload, String actor);

    List<Map<String, Object>> listUsers(Long tenantId, Long setId);

    Map<String, Object> createUser(Long tenantId, Long setId, Map<String, Object> payload);

    Map<String, Object> getUser(Long tenantId, Long id);

    Map<String, Object> preview(Long tenantId, Long id);
}
