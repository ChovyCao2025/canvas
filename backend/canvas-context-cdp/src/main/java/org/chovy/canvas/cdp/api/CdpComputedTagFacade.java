package org.chovy.canvas.cdp.api;

import java.util.Map;

public interface CdpComputedTagFacade {

    Map<String, Object> list(Long tenantId);

    Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> preview(Long tenantId, String tagCode);

    Map<String, Object> activate(Long tenantId, String tagCode, String actor);

    Map<String, Object> pause(Long tenantId, String tagCode, String actor);

    Map<String, Object> runNow(Long tenantId, String tagCode, String actor);

    Map<String, Object> listRuns(Long tenantId, String tagCode, Integer limit);

    Map<String, Object> lineage(Long tenantId, String tagCode);

    Map<String, Object> impactCheck(Long tenantId, String tagCode, Map<String, Object> payload, String actor);
}
