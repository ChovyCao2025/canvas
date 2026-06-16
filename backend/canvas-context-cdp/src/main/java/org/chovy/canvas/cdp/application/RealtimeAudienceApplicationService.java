package org.chovy.canvas.cdp.application;

import java.util.List;

import org.chovy.canvas.cdp.api.RealtimeAudienceFacade;
import org.chovy.canvas.cdp.domain.RealtimeAudienceCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排 RealtimeAudience 的应用服务流程。
 */
@Service
public class RealtimeAudienceApplicationService implements RealtimeAudienceFacade {

    /**
     * 执行 RealtimeAudienceCatalog 对应的 CDP 业务操作。
     */
    private final RealtimeAudienceCatalog catalog = new RealtimeAudienceCatalog();

    /**
     * 执行 processEvent 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventResult processEvent(Long tenantId, Long audienceId, CdpEvent event, boolean removeOnNoMatch) {
        return catalog.processEvent(safeTenantId(tenantId), audienceId, event, removeOnNoMatch);
    }

    /**
     * 创建Snapshot。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SnapshotResult createSnapshot(Long tenantId, Long audienceId, String reason, String actor) {
        return catalog.createSnapshot(safeTenantId(tenantId), audienceId, reason, actorOrDefault(actor));
    }

    /**
     * 查询Snapshots列表。
     */
    @Override
    public List<SnapshotRow> listSnapshots(Long tenantId, Long audienceId, int limit) {
        return catalog.listSnapshots(safeTenantId(tenantId), audienceId, normalizeLimit(limit));
    }

    /**
     * 执行 overlap 对应的 CDP 业务操作。
     */
    @Override
    public OverlapResult overlap(Long leftId, Long rightId) {
        return catalog.overlap(leftId, rightId);
    }

    /**
     * 执行 merge 对应的 CDP 业务操作。
     */
    @Override
    public SetOperationResult merge(Long leftId, Long rightId) {
        return catalog.merge(leftId, rightId);
    }

    /**
     * 执行 exclude 对应的 CDP 业务操作。
     */
    @Override
    public SetOperationResult exclude(Long baseId, Long excludedId) {
        return catalog.exclude(baseId, excludedId);
    }

    /**
     * 返回安全的Tenant Id。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 执行 actorOrDefault 对应的 CDP 业务操作。
     */
    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    /**
     * 归一化Limit。
     */
    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 100;
        }
        return Math.min(limit, 500);
    }
}
