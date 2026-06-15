package org.chovy.canvas.execution.application;

import org.chovy.canvas.execution.api.ExecutionRequestFacade;
import org.chovy.canvas.execution.domain.ExecutionRequestCatalog;
import org.springframework.stereotype.Service;

@Service
public class ExecutionRequestApplicationService implements ExecutionRequestFacade {

    private final ExecutionRequestCatalog catalog;

    public ExecutionRequestApplicationService() {
        this(new ExecutionRequestCatalog());
    }

    public ExecutionRequestApplicationService(ExecutionRequestCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public RequestPageView list(RequestQuery query) {
        return catalog.list(query);
    }

    @Override
    public ReplayResult replay(String id, ReplayCommand command) {
        return catalog.replay(id, command);
    }

    @Override
    public BatchReplayResult replayBatch(BatchReplayCommand command) {
        return catalog.replayBatch(command);
    }

    @Override
    public void register(RequestCommand command) {
        catalog.register(command);
    }
}
