package org.chovy.canvas.cdp.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseSloPolicyFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseSloPolicyCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排 CdpWarehouseSloPolicy 的应用服务流程。
 */
@Service
public class CdpWarehouseSloPolicyApplicationService implements CdpWarehouseSloPolicyFacade {

    /**
     * 执行 CdpWarehouseSloPolicyCatalog 对应的 CDP 业务操作。
     */
    private final CdpWarehouseSloPolicyCatalog catalog = new CdpWarehouseSloPolicyCatalog();

    /**
     * 查询Policies列表。
     */
    @Override
    public List<Map<String, Object>> listPolicies(Long tenantId, String status) {
        return catalog.listPolicies(tenantIdOrDefault(tenantId), status);
    }

    /**
     * 执行 effectivePolicy 对应的 CDP 业务操作。
     */
    @Override
    public Map<String, Object> effectivePolicy(Long tenantId, String policyKey) {
        return catalog.effectivePolicy(tenantIdOrDefault(tenantId), policyKey);
    }

    /**
     * 执行 upsertPolicy 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertPolicy(Long tenantId, Map<String, Object> payload) {
        return catalog.upsertPolicy(tenantIdOrDefault(tenantId), payload == null ? Map.of() : payload);
    }

    /**
     * 执行 tenantIdOrDefault 对应的 CDP 业务操作。
     */
    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }
}
