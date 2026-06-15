package org.chovy.canvas.marketing.application;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.ProgrammaticDspFacade;
import org.chovy.canvas.marketing.domain.ProgrammaticDspCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgrammaticDspApplicationService implements ProgrammaticDspFacade {

    private final ProgrammaticDspCatalog catalog;

    public ProgrammaticDspApplicationService() {
        this(Clock.systemDefaultZone());
    }

    ProgrammaticDspApplicationService(Clock clock) {
        this.catalog = new ProgrammaticDspCatalog(clock);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertSeat(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertSeat(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertCampaign(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertCampaign(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertLineItem(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertLineItem(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertSupplyPath(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertSupplyPath(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> recordSnapshot(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.recordSnapshot(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    @Override
    public Map<String, Object> summary(Long tenantId, Map<String, Object> query) {
        return catalog.summary(safeTenantId(tenantId), query);
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
    public List<Map<String, Object>> listMutations(Long tenantId, Map<String, Object> query) {
        return catalog.listMutations(safeTenantId(tenantId), query);
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }
}
