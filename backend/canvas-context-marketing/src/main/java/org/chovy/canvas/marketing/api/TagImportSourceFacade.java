package org.chovy.canvas.marketing.api;

import java.util.Map;

/**
 * 定义TagImportSourceFacade的营销上下文访问契约。
 */
public interface TagImportSourceFacade {

    /**
     * 查询sources列表。
     */
    Map<String, Object> listSources(Long tenantId, Integer enabled);

    /**
     * 创建source业务对象。
     */
    Map<String, Object> createSource(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 更新source业务对象。
     */
    Map<String, Object> updateSource(Long tenantId, Long id, Map<String, Object> payload, String actor);

    /**
     * 删除或停用source业务对象。
     */
    void deleteSource(Long tenantId, Long id);

    /**
     * 执行runSource业务操作。
     */
    Map<String, Object> runSource(Long tenantId, Long id);
}
