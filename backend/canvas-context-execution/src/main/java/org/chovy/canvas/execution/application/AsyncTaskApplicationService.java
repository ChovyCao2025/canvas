package org.chovy.canvas.execution.application;

import java.util.List;

import org.chovy.canvas.execution.api.AsyncTaskFacade;
import org.chovy.canvas.execution.domain.AsyncTaskCatalog;
import org.springframework.stereotype.Service;

@Service
public class AsyncTaskApplicationService implements AsyncTaskFacade {

    private final AsyncTaskCatalog catalog;

    public AsyncTaskApplicationService() {
        this(new AsyncTaskCatalog());
    }

    AsyncTaskApplicationService(AsyncTaskCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public List<AsyncTaskView> listTasks(AsyncTaskQuery query) {
        return catalog.listTasks(query);
    }

    @Override
    public AsyncTaskView getTask(String taskId, String username, boolean admin) {
        return catalog.getTask(taskId, username, admin);
    }
}
