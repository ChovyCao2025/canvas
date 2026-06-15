package org.chovy.canvas.cdp.application;

import java.time.LocalDateTime;

import org.chovy.canvas.cdp.api.CdpWarehouseOfflineRetentionFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseOfflineRetentionCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CdpWarehouseOfflineRetentionApplicationService implements CdpWarehouseOfflineRetentionFacade {

    private final CdpWarehouseOfflineRetentionCatalog catalog = new CdpWarehouseOfflineRetentionCatalog();

    @Override
    public OfflineCyclePlanView offlineCyclePlan(Long tenantId, LocalDateTime now, int backfillLimit,
                                                 int aggregationWindowMinutes) {
        return catalog.offlineCyclePlan(tenantIdOrDefault(tenantId), now, backfillLimit, aggregationWindowMinutes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OfflineCycleResultView runOfflineCycle(Long tenantId, LocalDateTime now, int backfillLimit,
                                                  int aggregationWindowMinutes, String operator) {
        return catalog.runOfflineCycle(tenantIdOrDefault(tenantId), now, backfillLimit, aggregationWindowMinutes,
                operator);
    }

    @Override
    public RetentionPlanView retentionPlan(Long tenantId, LocalDateTime now, int syncRunRetentionDays,
                                           int realtimeRetryRetentionDays, int resolvedIncidentRetentionDays) {
        return catalog.retentionPlan(tenantIdOrDefault(tenantId), now, syncRunRetentionDays,
                realtimeRetryRetentionDays, resolvedIncidentRetentionDays);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RetentionCleanupResultView runRetention(Long tenantId, LocalDateTime now, int syncRunRetentionDays,
                                                   int realtimeRetryRetentionDays, int resolvedIncidentRetentionDays,
                                                   String operator) {
        return catalog.runRetention(tenantIdOrDefault(tenantId), now, syncRunRetentionDays,
                realtimeRetryRetentionDays, resolvedIncidentRetentionDays, operator);
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }
}
