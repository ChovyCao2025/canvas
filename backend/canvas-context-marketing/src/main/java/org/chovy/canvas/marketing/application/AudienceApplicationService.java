package org.chovy.canvas.marketing.application;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.AudienceFacade;
import org.chovy.canvas.marketing.domain.AudienceCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排Audience相关的应用层用例。
 */
@Service
public class AudienceApplicationService implements AudienceFacade {

    /**
     * 承载该应用服务的内存目录。
     */
    private final AudienceCatalog catalog;

    /**
     * 创建AudienceApplicationService实例。
     */
    public AudienceApplicationService() {
        this(Clock.systemDefaultZone());
    }

    AudienceApplicationService(Clock clock) {
        this.catalog = new AudienceCatalog(clock);
    }

    /**
     * 查询列表。
     */
    @Override
    public Map<String, Object> list(Long tenantId, Integer page, Integer size) {
        return catalog.list(safeTenantId(tenantId), normalizedPage(page), normalizedSize(size));
    }

    /**
     * 执行sourceFields业务操作。
     */
    @Override
    public List<Map<String, Object>> sourceFields(String dataSourceType) {
        return catalog.sourceFields(dataSourceType);
    }

    /**
     * 执行preview业务操作。
     */
    @Override
    public Map<String, Object> preview(Long tenantId, Map<String, Object> payload) {
        return catalog.preview(safeTenantId(tenantId), payload);
    }

    /**
     * 返回字段值。
     */
    @Override
    public Map<String, Object> get(Long tenantId, Long id) {
        return catalog.get(safeTenantId(tenantId), id);
    }

    /**
     * 执行ready业务操作。
     */
    @Override
    public List<Map<String, Object>> ready(Long tenantId) {
        return catalog.ready(safeTenantId(tenantId));
    }

    /**
     * 创建业务对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.create(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    /**
     * 更新业务对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> update(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return catalog.update(safeTenantId(tenantId), id, payload, actorOrDefault(actor));
    }

    /**
     * 删除或停用业务对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> delete(Long tenantId, Long id) {
        return catalog.delete(safeTenantId(tenantId), id);
    }

    /**
     * 执行compute业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> compute(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return catalog.compute(safeTenantId(tenantId), id, payload, actorOrDefault(actor));
    }

    /**
     * 执行stat业务操作。
     */
    @Override
    public Map<String, Object> stat(Long tenantId, Long id) {
        return catalog.stat(safeTenantId(tenantId), id);
    }

    /**
     * 执行safeTenantId业务操作。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 执行actorOrDefault业务操作。
     */
    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    /**
     * 规范化dPage输入值。
     */
    private static int normalizedPage(Integer page) {
        return page == null ? 1 : Math.max(1, page);
    }

    /**
     * 规范化dSize输入值。
     */
    private static int normalizedSize(Integer size) {
        if (size == null) {
            return 20;
        }
        return Math.max(1, Math.min(size, 100));
    }
}
