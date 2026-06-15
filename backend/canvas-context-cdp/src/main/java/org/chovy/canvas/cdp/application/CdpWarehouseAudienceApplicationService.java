package org.chovy.canvas.cdp.application;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseAudienceFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseAudienceCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CdpWarehouseAudienceApplicationService implements CdpWarehouseAudienceFacade {

    private final CdpWarehouseAudienceCatalog catalog;

    public CdpWarehouseAudienceApplicationService() {
        this(Clock.systemUTC());
    }

    CdpWarehouseAudienceApplicationService(Clock clock) {
        this.catalog = new CdpWarehouseAudienceCatalog(clock);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> materialize(Long tenantId, Long audienceId, String actor) {
        return catalog.materialize(safeTenantId(tenantId), requiredAudienceId(audienceId), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> materializeGated(
            Long tenantId,
            Long audienceId,
            Map<String, Object> payload,
            String actor) {
        return catalog.materializeGated(safeTenantId(tenantId), requiredAudienceId(audienceId), safePayload(payload),
                actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> materializeContractGated(
            Long tenantId,
            Long audienceId,
            Map<String, Object> payload,
            String actor) {
        return catalog.materializeContractGated(safeTenantId(tenantId), requiredAudienceId(audienceId),
                safePayload(payload), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> rollback(Long tenantId, Long audienceId, Map<String, Object> payload, String actor) {
        return catalog.rollback(safeTenantId(tenantId), requiredAudienceId(audienceId), safePayload(payload),
                actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> refreshDue(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.refreshDue(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> refreshDueGated(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.refreshDue(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor), true);
    }

    @Override
    public List<Map<String, Object>> recentRuns(Long tenantId, Long audienceId, String status, Integer limit) {
        return catalog.recentRuns(safeTenantId(tenantId), audienceId, status, limit);
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static Long requiredAudienceId(Long audienceId) {
        if (audienceId == null || audienceId < 1) {
            throw new IllegalArgumentException("audienceId is required");
        }
        return audienceId;
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }
}
