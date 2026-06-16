package org.chovy.canvas.marketing.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MarketingFormFacade;
import org.chovy.canvas.marketing.domain.MarketingFormCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排MarketingForm相关的应用层用例。
 */
@Service
public class MarketingFormApplicationService implements MarketingFormFacade {

    /**
     * 保存DEFAULT_TENANT_ID字段值。
     */
    private static final Long DEFAULT_TENANT_ID = 7L;

    /**
     * 保存DEFAULT_ACTOR字段值。
     */
    private static final String DEFAULT_ACTOR = "operator-1";

    /**
     * 承载该应用服务的内存目录。
     */
    private final MarketingFormCatalog catalog;

    /**
     * 创建MarketingFormApplicationService实例。
     */
    public MarketingFormApplicationService() {
        this(new MarketingFormCatalog());
    }

    /**
     * 创建MarketingFormApplicationService实例。
     */
    public MarketingFormApplicationService(MarketingFormCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 查询forms列表。
     */
    @Override
    public List<Map<String, Object>> listForms(Long tenantId) {
        return catalog.listForms(safeTenantId(tenantId));
    }

    /**
     * 返回form字段值。
     */
    @Override
    public Map<String, Object> getForm(Long tenantId, Long id) {
        return catalog.getForm(safeTenantId(tenantId), id);
    }

    /**
     * 创建form业务对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createForm(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createForm(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 更新form业务对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateForm(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return catalog.updateForm(safeTenantId(tenantId), id, safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 设置status字段值。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> setStatus(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return catalog.setStatus(safeTenantId(tenantId), id, safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 执行submissions业务操作。
     */
    @Override
    public List<Map<String, Object>> submissions(Long tenantId, Long formId, Integer limit) {
        return catalog.submissions(safeTenantId(tenantId), formId, normalizedLimit(limit));
    }

    /**
     * 执行safeTenantId业务操作。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? DEFAULT_TENANT_ID : tenantId;
    }

    /**
     * 执行safePayload业务操作。
     */
    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    /**
     * 执行actorOrDefault业务操作。
     */
    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    /**
     * 规范化dLimit输入值。
     */
    private static int normalizedLimit(Integer limit) {
        if (limit == null) {
            return 50;
        }
        if (limit <= 0) {
            return 50;
        }
        return Math.min(limit, 100);
    }
}
