package org.chovy.canvas.canvas.application;

import org.chovy.canvas.canvas.api.EventDefinitionFacade;
import org.chovy.canvas.canvas.domain.EventDefinitionCatalog;
import org.springframework.stereotype.Service;

/**
 * 封装EventDefinitionApplicationService相关的业务逻辑。
 */
@Service
public class EventDefinitionApplicationService implements EventDefinitionFacade {

    /**
     * 保存catalog。
     */
    private final EventDefinitionCatalog catalog;

    /**
     * 创建当前对象实例。
     */
    public EventDefinitionApplicationService() {
        this(new EventDefinitionCatalog());
    }

    /**
     * 创建当前对象实例。
     */
    EventDefinitionApplicationService(EventDefinitionCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 列出。
     */
    @Override
    public PageView<EventDefinitionView> list(EventDefinitionListQuery query) {
        return catalog.list(query == null ? new EventDefinitionListQuery(1, 20, null) : query);
    }

    /**
     * 创建。
     */
    @Override
    public EventDefinitionView create(EventDefinitionCommand command) {
        return catalog.create(command);
    }

    /**
     * 更新。
     */
    @Override
    public EventDefinitionView update(Long id, EventDefinitionCommand command) {
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
