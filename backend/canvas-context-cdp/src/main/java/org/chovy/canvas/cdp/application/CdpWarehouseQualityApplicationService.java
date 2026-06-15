package org.chovy.canvas.cdp.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseQualityFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseQualityCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CdpWarehouseQualityApplicationService implements CdpWarehouseQualityFacade {

    private final CdpWarehouseQualityCatalog catalog = new CdpWarehouseQualityCatalog();

    @Override
    public List<Map<String, Object>> recentChecks(Long tenantId, int limit) {
        return catalog.recentChecks(tenantIdOrDefault(tenantId), limit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reconcileOds(Long tenantId, LocalDateTime from, LocalDateTime to, Long tolerance,
                                            String operator) {
        return catalog.reconcileOds(tenantIdOrDefault(tenantId), from, to, tolerance, operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> checkAggregateLag(Long tenantId, LocalDateTime now, Long maxLagMinutes,
                                                 String operator) {
        return catalog.checkAggregateLag(tenantIdOrDefault(tenantId), now, maxLagMinutes, operator);
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }
}
