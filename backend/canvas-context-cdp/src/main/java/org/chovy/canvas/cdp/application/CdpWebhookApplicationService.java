package org.chovy.canvas.cdp.application;

import java.time.Clock;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWebhookFacade;
import org.chovy.canvas.cdp.domain.CdpWebhookCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排 CdpWebhook 的应用服务流程。
 */
@Service
public class CdpWebhookApplicationService implements CdpWebhookFacade {

    /**
     * 领域目录组件。
     */
    private final CdpWebhookCatalog catalog;

    /**
     * 创建当前组件实例。
     */
    public CdpWebhookApplicationService() {
        this(Clock.systemDefaultZone());
    }

    CdpWebhookApplicationService(Clock clock) {
        this.catalog = new CdpWebhookCatalog(clock);
    }

    /**
     * 查询list列表。
     */
    @Override
    public Map<String, Object> list(Long tenantId) {
        return catalog.list(safeTenantId(tenantId));
    }

    /**
     * 创建create。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.create(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 更新update。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> update(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return catalog.update(safeTenantId(tenantId), id, safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 执行 pause 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> pause(Long tenantId, Long id, String actor) {
        return catalog.transition(safeTenantId(tenantId), id, "PAUSED", actorOrDefault(actor));
    }

    /**
     * 执行 resume 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> resume(Long tenantId, Long id, String actor) {
        return catalog.transition(safeTenantId(tenantId), id, "ACTIVE", actorOrDefault(actor));
    }

    /**
     * 执行 disable 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> disable(Long tenantId, Long id, String actor) {
        return catalog.transition(safeTenantId(tenantId), id, "DISABLED", actorOrDefault(actor));
    }

    /**
     * 执行 rotateSecret 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> rotateSecret(Long tenantId, Long id, String actor) {
        return catalog.rotateSecret(safeTenantId(tenantId), id, actorOrDefault(actor));
    }

    /**
     * 执行 testDelivery 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> testDelivery(Long tenantId, Long id, String actor) {
        return catalog.testDelivery(safeTenantId(tenantId), id, actorOrDefault(actor));
    }

    /**
     * 执行 deliveries 对应的 CDP 业务操作。
     */
    @Override
    public Map<String, Object> deliveries(Long tenantId, Long id) {
        return deliveries(tenantId, id, null);
    }

    /**
     * 执行 deliveries 对应的 CDP 业务操作。
     */
    @Override
    public Map<String, Object> deliveries(Long tenantId, Long id, Integer limit) {
        return catalog.deliveries(safeTenantId(tenantId), id, normalizeLimit(limit));
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
     * 返回安全的Payload。
     */
    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    /**
     * 归一化Limit。
     */
    private static Integer normalizeLimit(Integer limit) {
        return limit == null || limit <= 0 ? 100 : Math.min(limit, 500);
    }
}
