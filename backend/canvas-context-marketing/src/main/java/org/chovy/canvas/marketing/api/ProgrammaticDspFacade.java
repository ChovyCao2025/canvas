package org.chovy.canvas.marketing.api;

import java.util.List;
import java.util.Map;

/**
 * 定义ProgrammaticDspFacade的营销上下文访问契约。
 */
public interface ProgrammaticDspFacade {

    /**
     * 执行upsertSeat业务操作。
     */
    Map<String, Object> upsertSeat(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 执行upsertCampaign业务操作。
     */
    Map<String, Object> upsertCampaign(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 执行upsertLineItem业务操作。
     */
    Map<String, Object> upsertLineItem(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 执行upsertSupplyPath业务操作。
     */
    Map<String, Object> upsertSupplyPath(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 执行recordSnapshot业务操作。
     */
    Map<String, Object> recordSnapshot(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 执行summary业务操作。
     */
    Map<String, Object> summary(Long tenantId, Map<String, Object> query);

    /**
     * 执行proposeMutation业务操作。
     */
    Map<String, Object> proposeMutation(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 执行approveMutation业务操作。
     */
    Map<String, Object> approveMutation(Long tenantId, Long mutationId, Map<String, Object> payload, String actor);

    /**
     * 执行executeMutation业务操作。
     */
    Map<String, Object> executeMutation(Long tenantId, Long mutationId, Map<String, Object> payload, String actor);

    /**
     * 查询mutations列表。
     */
    List<Map<String, Object>> listMutations(Long tenantId, Map<String, Object> query);
}
