package org.chovy.canvas.cdp.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseFieldGovernanceFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseFieldGovernanceCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排 CdpWarehouseFieldGovernance 的应用服务流程。
 */
@Service
public class CdpWarehouseFieldGovernanceApplicationService implements CdpWarehouseFieldGovernanceFacade {

    /**
     * 执行 CdpWarehouseFieldGovernanceCatalog 对应的 CDP 业务操作。
     */
    private final CdpWarehouseFieldGovernanceCatalog catalog = new CdpWarehouseFieldGovernanceCatalog();

    /**
     * 查询Policies列表。
     */
    @Override
    public List<Map<String, Object>> listPolicies(Long tenantId, String datasetKey, String lifecycleStatus) {
        return catalog.listPolicies(tenantIdOrDefault(tenantId), datasetKey, lifecycleStatus);
    }

    /**
     * 执行 upsertPolicy 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertPolicy(Long tenantId, Map<String, Object> payload) {
        return catalog.upsertPolicy(tenantIdOrDefault(tenantId), safePayload(payload));
    }

    /**
     * 执行 evaluateBiQuery 对应的 CDP 业务操作。
     */
    @Override
    public Map<String, Object> evaluateBiQuery(Long tenantId, String actor, String role, Map<String, Object> request) {
        return catalog.evaluateBiQuery(tenantIdOrDefault(tenantId), actorOrDefault(actor), roleOrDefault(role),
                safePayload(request));
    }

    /**
     * 执行 tenantIdOrDefault 对应的 CDP 业务操作。
     */
    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 执行 actorOrDefault 对应的 CDP 业务操作。
     */
    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    /**
     * 执行 roleOrDefault 对应的 CDP 业务操作。
     */
    private static String roleOrDefault(String role) {
        return role == null || role.isBlank() ? "OPERATOR" : role.trim();
    }

    /**
     * 返回安全的Payload。
     */
    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }
}
