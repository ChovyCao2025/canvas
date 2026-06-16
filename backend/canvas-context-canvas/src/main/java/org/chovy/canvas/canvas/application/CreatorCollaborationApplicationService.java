package org.chovy.canvas.canvas.application;

import java.time.Clock;
import java.util.Map;

import org.chovy.canvas.canvas.api.CreatorCollaborationFacade;
import org.chovy.canvas.canvas.domain.CreatorCollaborationCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 封装CreatorCollaborationApplicationService相关的业务逻辑。
 */
@Service
public class CreatorCollaborationApplicationService implements CreatorCollaborationFacade {

    /**
     * 保存catalog。
     */
    private final CreatorCollaborationCatalog catalog;

    /**
     * 创建当前对象实例。
     */
    public CreatorCollaborationApplicationService() {
        this(Clock.systemDefaultZone());
    }

    /**
     * 创建当前对象实例。
     */
    CreatorCollaborationApplicationService(Clock clock) {
        this.catalog = new CreatorCollaborationCatalog(clock);
    }

    /**
     * 处理upsertCreator。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertCreator(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertCreator(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    /**
     * 处理upsertCampaign。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertCampaign(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertCampaign(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    /**
     * 处理upsertCollaboration。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertCollaboration(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertCollaboration(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    /**
     * 处理upsertDeliverable。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertDeliverable(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertDeliverable(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    /**
     * 处理proposeMutation。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> proposeMutation(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.proposeMutation(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 审批创作者协作变更。
     */
    public Map<String, Object> approveMutation(Long tenantId, Long mutationId, Map<String, Object> payload,
                                               String actor) {
        return catalog.approveMutation(safeTenantId(tenantId), mutationId, payload, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 执行已审批的创作者协作变更。
     */
    public Map<String, Object> executeMutation(Long tenantId, Long mutationId, Map<String, Object> payload,
                                               String actor) {
        return catalog.executeMutation(safeTenantId(tenantId), mutationId, payload, actorOrDefault(actor));
    }

    /**
     * 列出Mutations。
     */
    @Override
    public Map<String, Object> listMutations(Long tenantId, Map<String, Object> query) {
        return catalog.listMutations(safeTenantId(tenantId), query);
    }

    /**
     * 处理summary。
     */
    @Override
    public Map<String, Object> summary(Long tenantId, Map<String, Object> query) {
        return catalog.summary(safeTenantId(tenantId), query);
    }

    /**
     * 处理safe tenant标识。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 处理actorOrDefault。
     */
    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }
}
