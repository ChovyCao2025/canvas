package org.chovy.canvas.cdp.application;

import java.time.Clock;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpComputedTagFacade;
import org.chovy.canvas.cdp.domain.CdpComputedTagCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排 CdpComputedTag 的应用服务流程。
 */
@Service
public class CdpComputedTagApplicationService implements CdpComputedTagFacade {

    /**
     * 领域目录组件。
     */
    private final CdpComputedTagCatalog catalog;

    /**
     * 创建当前组件实例。
     */
    public CdpComputedTagApplicationService() {
        this(Clock.systemDefaultZone());
    }

    CdpComputedTagApplicationService(Clock clock) {
        this.catalog = new CdpComputedTagCatalog(clock);
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
     * 执行 preview 对应的 CDP 业务操作。
     */
    @Override
    public Map<String, Object> preview(Long tenantId, String tagCode) {
        return catalog.preview(safeTenantId(tenantId), tagCode);
    }

    /**
     * 执行 activate 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> activate(Long tenantId, String tagCode, String actor) {
        return catalog.activate(safeTenantId(tenantId), tagCode, actorOrDefault(actor));
    }

    /**
     * 执行 pause 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> pause(Long tenantId, String tagCode, String actor) {
        return catalog.pause(safeTenantId(tenantId), tagCode, actorOrDefault(actor));
    }

    /**
     * 执行 runNow 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> runNow(Long tenantId, String tagCode, String actor) {
        return catalog.runNow(safeTenantId(tenantId), tagCode, actorOrDefault(actor));
    }

    /**
     * 查询Runs列表。
     */
    @Override
    public Map<String, Object> listRuns(Long tenantId, String tagCode, Integer limit) {
        return catalog.listRuns(safeTenantId(tenantId), tagCode, normalizeLimit(limit));
    }

    /**
     * 执行 lineage 对应的 CDP 业务操作。
     */
    @Override
    public Map<String, Object> lineage(Long tenantId, String tagCode) {
        return catalog.lineage(safeTenantId(tenantId), tagCode);
    }

    /**
     * 执行 impactCheck 对应的 CDP 业务操作。
     */
    @Override
    public Map<String, Object> impactCheck(Long tenantId, String tagCode, Map<String, Object> payload, String actor) {
        return catalog.impactCheck(safeTenantId(tenantId), tagCode, safePayload(payload), actorOrDefault(actor));
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
