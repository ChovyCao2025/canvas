package org.chovy.canvas.cdp.application;

import java.util.List;

import org.chovy.canvas.cdp.api.RealtimeAudienceFacade;
import org.chovy.canvas.cdp.domain.RealtimeAudienceCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RealtimeAudienceApplicationService implements RealtimeAudienceFacade {

    private final RealtimeAudienceCatalog catalog = new RealtimeAudienceCatalog();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventResult processEvent(Long tenantId, Long audienceId, CdpEvent event, boolean removeOnNoMatch) {
        return catalog.processEvent(safeTenantId(tenantId), audienceId, event, removeOnNoMatch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SnapshotResult createSnapshot(Long tenantId, Long audienceId, String reason, String actor) {
        return catalog.createSnapshot(safeTenantId(tenantId), audienceId, reason, actorOrDefault(actor));
    }

    @Override
    public List<SnapshotRow> listSnapshots(Long tenantId, Long audienceId, int limit) {
        return catalog.listSnapshots(safeTenantId(tenantId), audienceId, normalizeLimit(limit));
    }

    @Override
    public OverlapResult overlap(Long leftId, Long rightId) {
        return catalog.overlap(leftId, rightId);
    }

    @Override
    public SetOperationResult merge(Long leftId, Long rightId) {
        return catalog.merge(leftId, rightId);
    }

    @Override
    public SetOperationResult exclude(Long baseId, Long excludedId) {
        return catalog.exclude(baseId, excludedId);
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 100;
        }
        return Math.min(limit, 500);
    }
}
