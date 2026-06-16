package org.chovy.canvas.cdp.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseQualityFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseQualityCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排 CdpWarehouseQuality 的应用服务流程。
 */
@Service
public class CdpWarehouseQualityApplicationService implements CdpWarehouseQualityFacade {

    /**
     * 执行 CdpWarehouseQualityCatalog 对应的 CDP 业务操作。
     */
    private final CdpWarehouseQualityCatalog catalog = new CdpWarehouseQualityCatalog();

    /**
     * 执行 recentChecks 对应的 CDP 业务操作。
     */
    @Override
    public List<Map<String, Object>> recentChecks(Long tenantId, int limit) {
        return catalog.recentChecks(tenantIdOrDefault(tenantId), limit);
    }

    /**
     * 执行 reconcileOds 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reconcileOds(Long tenantId, LocalDateTime from, LocalDateTime to, Long tolerance,
                                            String operator) {
        return catalog.reconcileOds(tenantIdOrDefault(tenantId), from, to, tolerance, operator);
    }

    /**
     * 执行 checkAggregateLag 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> checkAggregateLag(Long tenantId, LocalDateTime now, Long maxLagMinutes,
                                                 String operator) {
        return catalog.checkAggregateLag(tenantIdOrDefault(tenantId), now, maxLagMinutes, operator);
    }

    /**
     * 执行 tenantIdOrDefault 对应的 CDP 业务操作。
     */
    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }
}
