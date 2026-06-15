package org.chovy.canvas.canvas.application;

import org.chovy.canvas.canvas.api.EventDefinitionFacade;
import org.chovy.canvas.canvas.domain.EventDefinitionCatalog;
import org.springframework.stereotype.Service;

@Service
public class EventDefinitionApplicationService implements EventDefinitionFacade {

    private final EventDefinitionCatalog catalog;

    public EventDefinitionApplicationService() {
        this(new EventDefinitionCatalog());
    }

    EventDefinitionApplicationService(EventDefinitionCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public PageView<EventDefinitionView> list(EventDefinitionListQuery query) {
        return catalog.list(query == null ? new EventDefinitionListQuery(1, 20, null) : query);
    }

    @Override
    public EventDefinitionView create(EventDefinitionCommand command) {
        return catalog.create(command);
    }

    @Override
    public EventDefinitionView update(Long id, EventDefinitionCommand command) {
        return catalog.update(id, command);
    }

    @Override
    public void delete(Long id) {
        catalog.delete(id);
    }
}
