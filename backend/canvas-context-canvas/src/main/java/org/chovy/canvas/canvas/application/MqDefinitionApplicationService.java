package org.chovy.canvas.canvas.application;

import org.chovy.canvas.canvas.api.MqDefinitionFacade;
import org.chovy.canvas.canvas.domain.MqDefinitionCatalog;
import org.springframework.stereotype.Service;

/**
 * 封装MqDefinitionApplicationService相关的业务逻辑。
 */
@Service
public class MqDefinitionApplicationService implements MqDefinitionFacade {

    /**
     * 保存catalog。
     */
    private final MqDefinitionCatalog catalog;

    /**
     * 创建当前对象实例。
     */
    public MqDefinitionApplicationService() {
        this(new MqDefinitionCatalog());
    }

    /**
     * 创建当前对象实例。
     */
    MqDefinitionApplicationService(MqDefinitionCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 列出。
     */
    @Override
    public PageView<MqDefinitionView> list(MqDefinitionListQuery query) {
        return catalog.list(query == null ? new MqDefinitionListQuery(1, 20, null) : query);
    }

    /**
     * 创建。
     */
    @Override
    public MqDefinitionView create(MqDefinitionCommand command) {
        return catalog.create(command);
    }

    /**
     * 更新。
     */
    @Override
    public MqDefinitionView update(Long id, MqDefinitionCommand command) {
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
