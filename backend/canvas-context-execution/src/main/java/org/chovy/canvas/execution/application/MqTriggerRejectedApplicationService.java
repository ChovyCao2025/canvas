package org.chovy.canvas.execution.application;

import org.chovy.canvas.execution.api.MqTriggerRejectedFacade;
import org.chovy.canvas.execution.domain.MqTriggerRejectedCatalog;
import org.springframework.stereotype.Service;

@Service
public class MqTriggerRejectedApplicationService implements MqTriggerRejectedFacade {

    private final MqTriggerRejectedCatalog catalog;

    public MqTriggerRejectedApplicationService() {
        this(new MqTriggerRejectedCatalog());
    }

    public MqTriggerRejectedApplicationService(MqTriggerRejectedCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public RejectedPageView list(RejectedQuery query) {
        return catalog.list(query);
    }

    @Override
    public RejectedView detail(Long id) {
        return catalog.detail(id);
    }

    @Override
    public ReplayResult replay(Long id) {
        return catalog.replay(id);
    }

    @Override
    public void register(RejectedCommand command) {
        catalog.register(command);
    }
}
