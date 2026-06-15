package org.chovy.canvas.cdp.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseDataPathProbeFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseDataPathProbeCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CdpWarehouseDataPathProbeApplicationService implements CdpWarehouseDataPathProbeFacade {

    private final CdpWarehouseDataPathProbeCatalog catalog = new CdpWarehouseDataPathProbeCatalog();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> run(Long tenantId, RunCommand command) {
        return catalog.run(tenantIdOrDefault(tenantId), command);
    }

    @Override
    public List<Map<String, Object>> recent(Long tenantId, int limit) {
        return catalog.recent(tenantIdOrDefault(tenantId), limit);
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }
}
