package org.chovy.canvas.cdp.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseDataPathProbeFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseDataPathProbeCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排 CdpWarehouseDataPathProbe 的应用服务流程。
 */
@Service
public class CdpWarehouseDataPathProbeApplicationService implements CdpWarehouseDataPathProbeFacade {

    /**
     * 执行 CdpWarehouseDataPathProbeCatalog 对应的 CDP 业务操作。
     */
    private final CdpWarehouseDataPathProbeCatalog catalog = new CdpWarehouseDataPathProbeCatalog();

    /**
     * 执行 run 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> run(Long tenantId, RunCommand command) {
        return catalog.run(tenantIdOrDefault(tenantId), command);
    }

    /**
     * 执行 recent 对应的 CDP 业务操作。
     */
    @Override
    public List<Map<String, Object>> recent(Long tenantId, int limit) {
        return catalog.recent(tenantIdOrDefault(tenantId), limit);
    }

    /**
     * 执行 tenantIdOrDefault 对应的 CDP 业务操作。
     */
    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }
}
