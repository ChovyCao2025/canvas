package org.chovy.canvas.marketing.api;

import java.util.List;
import java.util.Map;

public interface ProgrammaticDspFacade {

    Map<String, Object> upsertSeat(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> upsertCampaign(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> upsertLineItem(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> upsertSupplyPath(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> recordSnapshot(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> summary(Long tenantId, Map<String, Object> query);

    Map<String, Object> proposeMutation(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> approveMutation(Long tenantId, Long mutationId, Map<String, Object> payload, String actor);

    Map<String, Object> executeMutation(Long tenantId, Long mutationId, Map<String, Object> payload, String actor);

    List<Map<String, Object>> listMutations(Long tenantId, Map<String, Object> query);
}
