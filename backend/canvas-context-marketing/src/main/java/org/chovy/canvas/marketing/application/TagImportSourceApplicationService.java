package org.chovy.canvas.marketing.application;

import java.util.Map;

import org.chovy.canvas.marketing.api.TagImportSourceFacade;
import org.chovy.canvas.marketing.domain.TagImportSourceCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排TagImportSource相关的应用层用例。
 */
@Service
public class TagImportSourceApplicationService implements TagImportSourceFacade {

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
    private final TagImportSourceCatalog catalog;

    /**
     * 创建TagImportSourceApplicationService实例。
     */
    public TagImportSourceApplicationService() {
        this(new TagImportSourceCatalog());
    }

    /**
     * 创建TagImportSourceApplicationService实例。
     */
    public TagImportSourceApplicationService(TagImportSourceCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 查询sources列表。
     */
    @Override
    public Map<String, Object> listSources(Long tenantId, Integer enabled) {
        return catalog.listSources(tenantIdOrDefault(tenantId), enabled);
    }

    /**
     * 创建source业务对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createSource(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createSource(tenantIdOrDefault(tenantId), payload, actorOrDefault(actor));
    }

    /**
     * 更新source业务对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateSource(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return catalog.updateSource(tenantIdOrDefault(tenantId), id, payload, actorOrDefault(actor));
    }

    /**
     * 删除或停用source业务对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSource(Long tenantId, Long id) {
        catalog.deleteSource(tenantIdOrDefault(tenantId), id);
    }

    /**
     * 执行runSource业务操作。
     */
    @Override
    public Map<String, Object> runSource(Long tenantId, Long id) {
        return catalog.runSource(tenantIdOrDefault(tenantId), id);
    }

    /**
     * 执行tenantIdOrDefault业务操作。
     */
    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    /**
     * 执行actorOrDefault业务操作。
     */
    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }
}
