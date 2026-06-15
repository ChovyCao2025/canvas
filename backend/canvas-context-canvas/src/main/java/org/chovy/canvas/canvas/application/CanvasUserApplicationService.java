package org.chovy.canvas.canvas.application;

import java.util.List;

import org.chovy.canvas.canvas.api.CanvasUserFacade;
import org.chovy.canvas.canvas.domain.CanvasUserCatalog;
import org.springframework.stereotype.Service;

@Service
public class CanvasUserApplicationService implements CanvasUserFacade {

    private final CanvasUserCatalog catalog;

    public CanvasUserApplicationService() {
        this(new CanvasUserCatalog());
    }

    CanvasUserApplicationService(CanvasUserCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public List<CanvasUserView> listUsers(Long canvasId) {
        return catalog.listUsers(canvasId);
    }

    @Override
    public CanvasUserView getUserInCanvas(Long canvasId, String userId) {
        return catalog.getUserInCanvas(canvasId, userId);
    }

    @Override
    public List<CanvasExecutionView> listExecutions(Long canvasId, String userId) {
        return catalog.listExecutions(canvasId, userId);
    }

    @Override
    public void registerUser(Long canvasId, CanvasUserCommand command) {
        catalog.registerUser(canvasId, command);
    }

    @Override
    public void registerExecution(Long canvasId, String userId, ExecutionCommand command) {
        catalog.registerExecution(canvasId, userId, command);
    }
}
