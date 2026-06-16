package org.chovy.canvas.canvas.api;

import java.util.Map;

/**
 * 定义CreatorCollaborationFacade对外提供的能力契约。
 */
public interface CreatorCollaborationFacade {

    /**
     * 处理upsertCreator。
     */
    Map<String, Object> upsertCreator(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 处理upsertCampaign。
     */
    Map<String, Object> upsertCampaign(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 处理upsertCollaboration。
     */
    Map<String, Object> upsertCollaboration(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 处理upsertDeliverable。
     */
    Map<String, Object> upsertDeliverable(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 处理proposeMutation。
     */
    Map<String, Object> proposeMutation(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 处理approveMutation。
     */
    Map<String, Object> approveMutation(Long tenantId, Long mutationId, Map<String, Object> payload, String actor);

    /**
     * 处理executeMutation。
     */
    Map<String, Object> executeMutation(Long tenantId, Long mutationId, Map<String, Object> payload, String actor);

    /**
     * 列出Mutations。
     */
    Map<String, Object> listMutations(Long tenantId, Map<String, Object> query);

    /**
     * 处理summary。
     */
    Map<String, Object> summary(Long tenantId, Map<String, Object> query);
}
