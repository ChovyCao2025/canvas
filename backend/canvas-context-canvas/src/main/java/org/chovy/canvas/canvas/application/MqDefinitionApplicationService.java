package org.chovy.canvas.canvas.application;

import org.chovy.canvas.canvas.api.MqDefinitionFacade;
import org.chovy.canvas.canvas.domain.MqDefinitionCatalog;
import org.springframework.stereotype.Service;

@Service
public class MqDefinitionApplicationService implements MqDefinitionFacade {

    private final MqDefinitionCatalog catalog;

    public MqDefinitionApplicationService() {
        this(new MqDefinitionCatalog());
    }

    MqDefinitionApplicationService(MqDefinitionCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public PageView<MqDefinitionView> list(MqDefinitionListQuery query) {
        return catalog.list(query == null ? new MqDefinitionListQuery(1, 20, null) : query);
    }

    @Override
    public MqDefinitionView create(MqDefinitionCommand command) {
        return catalog.create(command);
    }

    @Override
    public MqDefinitionView update(Long id, MqDefinitionCommand command) {
        return catalog.update(id, command);
    }

    @Override
    public void delete(Long id) {
        catalog.delete(id);
    }
}
