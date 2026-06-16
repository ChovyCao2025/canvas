package org.chovy.canvas.cdp.application;

import java.time.Clock;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseTableFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseTableCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排 CdpWarehouseTable 的应用服务流程。
 */
@Service
public class CdpWarehouseTableApplicationService implements CdpWarehouseTableFacade {

    /**
     * 领域目录组件。
     */
    private final CdpWarehouseTableCatalog catalog;

    /**
     * 创建当前组件实例。
     */
    public CdpWarehouseTableApplicationService() {
        this(Clock.systemUTC());
    }

    /**
     * 创建当前组件实例。
     */
    public CdpWarehouseTableApplicationService(Clock clock) {
        this.catalog = new CdpWarehouseTableCatalog(clock);
    }

    /**
     * 查询Contracts列表。
     */
    @Override
    public Map<String, Object> listContracts(Long tenantId, String layer, String lifecycleStatus) {
        return catalog.listContracts(safeTenantId(tenantId), layer, lifecycleStatus);
    }

    /**
     * 执行 upsertContract 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertContract(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertContract(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 执行 inspectContract 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> inspectContract(Long tenantId, String tableKey, String actor, boolean live) {
        return catalog.inspectContract(safeTenantId(tenantId), tableKey, actorOrDefault(actor), live);
    }

    /**
     * 执行 inspectAll 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> inspectAll(Long tenantId, String actor, boolean live) {
        return catalog.inspectAll(safeTenantId(tenantId), actorOrDefault(actor), live);
    }

    /**
     * 执行 planRemediation 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> planRemediation(Long tenantId, String tableKey, boolean live, String actor) {
        return catalog.planRemediation(safeTenantId(tenantId), tableKey, live, actorOrDefault(actor));
    }

    /**
     * 执行 planAllRemediation 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> planAllRemediation(Long tenantId, boolean live, String actor) {
        return catalog.planAllRemediation(safeTenantId(tenantId), live, actorOrDefault(actor));
    }

    /**
     * 执行 scanIncidents 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> scanIncidents(Long tenantId, boolean live, String actor) {
        return catalog.scanIncidents(safeTenantId(tenantId), live, actorOrDefault(actor));
    }

    /**
     * 返回安全的Tenant Id。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 返回安全的Payload。
     */
    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    /**
     * 执行 actorOrDefault 对应的 CDP 业务操作。
     */
    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }
}
