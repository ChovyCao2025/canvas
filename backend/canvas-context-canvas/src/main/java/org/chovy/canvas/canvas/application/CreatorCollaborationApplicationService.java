package org.chovy.canvas.canvas.application;

import java.time.Clock;
import java.util.Map;

import org.chovy.canvas.canvas.api.CreatorCollaborationFacade;
import org.chovy.canvas.canvas.domain.CreatorCollaborationCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreatorCollaborationApplicationService implements CreatorCollaborationFacade {

    private final CreatorCollaborationCatalog catalog;

    public CreatorCollaborationApplicationService() {
        this(Clock.systemDefaultZone());
    }

    CreatorCollaborationApplicationService(Clock clock) {
        this.catalog = new CreatorCollaborationCatalog(clock);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertCreator(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertCreator(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertCampaign(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertCampaign(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertCollaboration(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertCollaboration(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertDeliverable(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertDeliverable(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> proposeMutation(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.proposeMutation(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> approveMutation(Long tenantId, Long mutationId, Map<String, Object> payload,
                                               String actor) {
        return catalog.approveMutation(safeTenantId(tenantId), mutationId, payload, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> executeMutation(Long tenantId, Long mutationId, Map<String, Object> payload,
                                               String actor) {
        return catalog.executeMutation(safeTenantId(tenantId), mutationId, payload, actorOrDefault(actor));
    }

    @Override
    public Map<String, Object> listMutations(Long tenantId, Map<String, Object> query) {
        return catalog.listMutations(safeTenantId(tenantId), query);
    }

    @Override
    public Map<String, Object> summary(Long tenantId, Map<String, Object> query) {
        return catalog.summary(safeTenantId(tenantId), query);
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }
}
