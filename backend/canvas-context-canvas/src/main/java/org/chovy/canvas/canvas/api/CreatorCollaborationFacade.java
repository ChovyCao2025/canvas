package org.chovy.canvas.canvas.api;

import java.util.Map;

public interface CreatorCollaborationFacade {

    Map<String, Object> upsertCreator(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> upsertCampaign(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> upsertCollaboration(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> upsertDeliverable(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> proposeMutation(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> approveMutation(Long tenantId, Long mutationId, Map<String, Object> payload, String actor);

    Map<String, Object> executeMutation(Long tenantId, Long mutationId, Map<String, Object> payload, String actor);

    Map<String, Object> listMutations(Long tenantId, Map<String, Object> query);

    Map<String, Object> summary(Long tenantId, Map<String, Object> query);
}
