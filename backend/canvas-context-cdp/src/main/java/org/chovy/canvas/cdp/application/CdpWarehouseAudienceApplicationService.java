package org.chovy.canvas.cdp.application;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseAudienceFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseAudienceCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排 CdpWarehouseAudience 的应用服务流程。
 */
@Service
public class CdpWarehouseAudienceApplicationService implements CdpWarehouseAudienceFacade {

    /**
     * 领域目录组件。
     */
    private final CdpWarehouseAudienceCatalog catalog;

    /**
     * 创建当前组件实例。
     */
    public CdpWarehouseAudienceApplicationService() {
        this(Clock.systemUTC());
    }

    CdpWarehouseAudienceApplicationService(Clock clock) {
        this.catalog = new CdpWarehouseAudienceCatalog(clock);
    }

    /**
     * 执行 materialize 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> materialize(Long tenantId, Long audienceId, String actor) {
        return catalog.materialize(safeTenantId(tenantId), requiredAudienceId(audienceId), actorOrDefault(actor));
    }

    /**
     * 执行 materializeGated 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> materializeGated(
            Long tenantId,
            Long audienceId,
            Map<String, Object> payload,
            String actor) {
        return catalog.materializeGated(safeTenantId(tenantId), requiredAudienceId(audienceId), safePayload(payload),
                actorOrDefault(actor));
    }

    /**
     * 执行 materializeContractGated 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> materializeContractGated(
            Long tenantId,
            Long audienceId,
            Map<String, Object> payload,
            String actor) {
        return catalog.materializeContractGated(safeTenantId(tenantId), requiredAudienceId(audienceId),
                safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 执行 rollback 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> rollback(Long tenantId, Long audienceId, Map<String, Object> payload, String actor) {
        return catalog.rollback(safeTenantId(tenantId), requiredAudienceId(audienceId), safePayload(payload),
                actorOrDefault(actor));
    }

    /**
     * 执行 refreshDue 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> refreshDue(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.refreshDue(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor), false);
    }

    /**
     * 执行 refreshDueGated 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> refreshDueGated(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.refreshDue(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor), true);
    }

    /**
     * 执行 recentRuns 对应的 CDP 业务操作。
     */
    @Override
    public List<Map<String, Object>> recentRuns(Long tenantId, Long audienceId, String status, Integer limit) {
        return catalog.recentRuns(safeTenantId(tenantId), audienceId, status, limit);
    }

    /**
     * 返回安全的Tenant Id。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 读取并校验必填的d Audience Id。
     */
    private static Long requiredAudienceId(Long audienceId) {
        if (audienceId == null || audienceId < 1) {
            throw new IllegalArgumentException("audienceId is required");
        }
        return audienceId;
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
