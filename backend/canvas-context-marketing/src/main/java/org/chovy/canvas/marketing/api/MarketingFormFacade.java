package org.chovy.canvas.marketing.api;

import java.util.List;
import java.util.Map;

/**
 * 定义MarketingFormFacade的营销上下文访问契约。
 */
public interface MarketingFormFacade {

    /**
     * 查询forms列表。
     */
    List<Map<String, Object>> listForms(Long tenantId);

    /**
     * 返回form字段值。
     */
    Map<String, Object> getForm(Long tenantId, Long id);

    /**
     * 创建form业务对象。
     */
    Map<String, Object> createForm(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 更新form业务对象。
     */
    Map<String, Object> updateForm(Long tenantId, Long id, Map<String, Object> payload, String actor);

    /**
     * 设置status字段值。
     */
    Map<String, Object> setStatus(Long tenantId, Long id, Map<String, Object> payload, String actor);

    /**
     * 执行submissions业务操作。
     */
    List<Map<String, Object>> submissions(Long tenantId, Long formId, Integer limit);
}
