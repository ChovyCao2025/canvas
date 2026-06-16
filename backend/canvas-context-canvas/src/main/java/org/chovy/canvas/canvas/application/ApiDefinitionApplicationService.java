package org.chovy.canvas.canvas.application;

import org.chovy.canvas.canvas.api.ApiDefinitionFacade;
import org.chovy.canvas.canvas.domain.ApiDefinitionCatalog;
import org.springframework.stereotype.Service;

/**
 * 封装ApiDefinitionApplicationService相关的业务逻辑。
 */
@Service
public class ApiDefinitionApplicationService implements ApiDefinitionFacade {

    /**
     * 保存catalog。
     */
    private final ApiDefinitionCatalog catalog;

    /**
     * 创建当前对象实例。
     */
    public ApiDefinitionApplicationService() {
        this(new ApiDefinitionCatalog());
    }

    /**
     * 创建当前对象实例。
     */
    ApiDefinitionApplicationService(ApiDefinitionCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 列出。
     */
    @Override
    public PageView<ApiDefinitionView> list(ApiDefinitionListQuery query) {
        return catalog.list(query == null ? new ApiDefinitionListQuery(1, 20, null) : query);
    }

    /**
     * 创建。
     */
    @Override
    public ApiDefinitionView create(ApiDefinitionCommand command) {
        return catalog.create(command);
    }

    /**
     * 更新。
     */
    @Override
    public ApiDefinitionView update(Long id, ApiDefinitionCommand command) {
        return catalog.update(id, command);
    }

    /**
     * 删除。
     */
    @Override
    public void delete(Long id) {
        catalog.delete(id);
    }
}
