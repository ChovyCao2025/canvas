package org.chovy.canvas.cdp.api;

import java.util.Map;

public interface CdpComputedProfileFacade {

    Map<String, Object> list(Long tenantId);

    Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> preview(Long tenantId, Long id);

    Map<String, Object> activate(Long tenantId, Long id, String actor);

    Map<String, Object> pause(Long tenantId, Long id, String actor);

    Map<String, Object> runNow(Long tenantId, Long id, String actor);

    Map<String, Object> listRuns(Long tenantId, Long id, Integer limit);

    Map<String, Object> listChanges(Long tenantId, Long id, String userId, Integer limit);
}
