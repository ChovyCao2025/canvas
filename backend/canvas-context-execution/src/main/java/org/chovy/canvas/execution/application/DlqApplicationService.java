package org.chovy.canvas.execution.application;

import org.chovy.canvas.execution.api.DlqFacade;
import org.chovy.canvas.execution.domain.DlqCatalog;
import org.springframework.stereotype.Service;

@Service
public class DlqApplicationService implements DlqFacade {

    private final DlqCatalog catalog;

    public DlqApplicationService() {
        this(new DlqCatalog());
    }

    public DlqApplicationService(DlqCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public DlqPageView list(DlqQuery query) {
        return catalog.list(query);
    }

    @Override
    public DlqReplayResult replay(Long id, boolean skipSuccessNodes) {
        return catalog.replay(id, skipSuccessNodes);
    }

    @Override
    public DeleteResult delete(Long id) {
        return catalog.delete(id);
    }

    @Override
    public void register(DlqEntryCommand command) {
        catalog.register(command);
    }
}
