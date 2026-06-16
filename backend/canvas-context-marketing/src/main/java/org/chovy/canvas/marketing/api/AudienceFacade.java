package org.chovy.canvas.marketing.api;

import java.util.List;
import java.util.Map;

/**
 * 定义AudienceFacade的营销上下文访问契约。
 */
public interface AudienceFacade {

    /**
     * 查询列表。
     */
    Map<String, Object> list(Long tenantId, Integer page, Integer size);

    /**
     * 执行sourceFields业务操作。
     */
    List<Map<String, Object>> sourceFields(String dataSourceType);

    /**
     * 执行preview业务操作。
     */
    Map<String, Object> preview(Long tenantId, Map<String, Object> payload);

    /**
     * 返回字段值。
     */
    Map<String, Object> get(Long tenantId, Long id);

    /**
     * 执行ready业务操作。
     */
    List<Map<String, Object>> ready(Long tenantId);

    /**
     * 创建业务对象。
     */
    Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 更新业务对象。
     */
    Map<String, Object> update(Long tenantId, Long id, Map<String, Object> payload, String actor);

    /**
     * 删除或停用业务对象。
     */
    Map<String, Object> delete(Long tenantId, Long id);

    /**
     * 执行compute业务操作。
     */
    Map<String, Object> compute(Long tenantId, Long id, Map<String, Object> payload, String actor);

    /**
     * 执行stat业务操作。
     */
    Map<String, Object> stat(Long tenantId, Long id);
}
