package org.chovy.canvas.execution.application;

import java.util.List;

import org.chovy.canvas.execution.api.AsyncTaskFacade;
import org.chovy.canvas.execution.domain.AsyncTaskCatalog;
import org.springframework.stereotype.Service;

/**
 * 定义 AsyncTaskApplicationService 的执行上下文数据结构或业务契约。
 */
@Service
public class AsyncTaskApplicationService implements AsyncTaskFacade {

    /**
     * 保存 catalog 对应的状态或配置。
     */
    private final AsyncTaskCatalog catalog;

    /**
     * 执行 AsyncTaskApplicationService 对应的业务处理。
     */
    public AsyncTaskApplicationService() {
        this(new AsyncTaskCatalog());
    }

    /**
     * 执行 AsyncTaskApplicationService 对应的业务处理。
     * @param catalog catalog 参数
     */
    AsyncTaskApplicationService(AsyncTaskCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 执行 listTasks 对应的业务处理。
     * @param query query 参数
     * @return 处理后的结果
     */
    @Override
    public List<AsyncTaskView> listTasks(AsyncTaskQuery query) {
        return catalog.listTasks(query);
    }

    /**
     * 执行 getTask 对应的业务处理。
     * @param taskId taskId 参数
     * @param username username 参数
     * @param admin admin 参数
     */
    @Override
    public AsyncTaskView getTask(String taskId, String username, boolean admin) {
        return catalog.getTask(taskId, username, admin);
    }
}
