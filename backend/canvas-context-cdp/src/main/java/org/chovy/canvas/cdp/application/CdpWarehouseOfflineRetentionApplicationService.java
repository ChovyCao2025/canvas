package org.chovy.canvas.cdp.application;

import java.time.LocalDateTime;

import org.chovy.canvas.cdp.api.CdpWarehouseOfflineRetentionFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseOfflineRetentionCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排 CdpWarehouseOfflineRetention 的应用服务流程。
 */
@Service
public class CdpWarehouseOfflineRetentionApplicationService implements CdpWarehouseOfflineRetentionFacade {

    /**
     * 执行 CdpWarehouseOfflineRetentionCatalog 对应的 CDP 业务操作。
     */
    private final CdpWarehouseOfflineRetentionCatalog catalog = new CdpWarehouseOfflineRetentionCatalog();

    /**
     * 执行 offlineCyclePlan 对应的 CDP 业务操作。
     */
    @Override
    public OfflineCyclePlanView offlineCyclePlan(Long tenantId, LocalDateTime now, int backfillLimit,
                                                 int aggregationWindowMinutes) {
        return catalog.offlineCyclePlan(tenantIdOrDefault(tenantId), now, backfillLimit, aggregationWindowMinutes);
    }

    /**
     * 执行 runOfflineCycle 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OfflineCycleResultView runOfflineCycle(Long tenantId, LocalDateTime now, int backfillLimit,
                                                  int aggregationWindowMinutes, String operator) {
        return catalog.runOfflineCycle(tenantIdOrDefault(tenantId), now, backfillLimit, aggregationWindowMinutes,
                operator);
    }

    /**
     * 执行 retentionPlan 对应的 CDP 业务操作。
     */
    @Override
    public RetentionPlanView retentionPlan(Long tenantId, LocalDateTime now, int syncRunRetentionDays,
                                           int realtimeRetryRetentionDays, int resolvedIncidentRetentionDays) {
        return catalog.retentionPlan(tenantIdOrDefault(tenantId), now, syncRunRetentionDays,
                realtimeRetryRetentionDays, resolvedIncidentRetentionDays);
    }

    /**
     * 执行 runRetention 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RetentionCleanupResultView runRetention(Long tenantId, LocalDateTime now, int syncRunRetentionDays,
                                                   int realtimeRetryRetentionDays, int resolvedIncidentRetentionDays,
                                                   String operator) {
        return catalog.runRetention(tenantIdOrDefault(tenantId), now, syncRunRetentionDays,
                realtimeRetryRetentionDays, resolvedIncidentRetentionDays, operator);
    }

    /**
     * 执行 tenantIdOrDefault 对应的 CDP 业务操作。
     */
    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }
}
