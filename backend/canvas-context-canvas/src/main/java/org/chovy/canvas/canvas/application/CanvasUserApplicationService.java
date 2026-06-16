package org.chovy.canvas.canvas.application;

import java.util.List;

import org.chovy.canvas.canvas.api.CanvasUserFacade;
import org.chovy.canvas.canvas.domain.CanvasUserCatalog;
import org.springframework.stereotype.Service;

/**
 * 封装CanvasUserApplicationService相关的业务逻辑。
 */
@Service
public class CanvasUserApplicationService implements CanvasUserFacade {

    /**
     * 保存catalog。
     */
    private final CanvasUserCatalog catalog;

    /**
     * 创建当前对象实例。
     */
    public CanvasUserApplicationService() {
        this(new CanvasUserCatalog());
    }

    /**
     * 创建当前对象实例。
     */
    CanvasUserApplicationService(CanvasUserCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 列出Users。
     */
    @Override
    public List<CanvasUserView> listUsers(Long canvasId) {
        return catalog.listUsers(canvasId);
    }

    /**
     * 获取UserInCanvas。
     */
    @Override
    public CanvasUserView getUserInCanvas(Long canvasId, String userId) {
        return catalog.getUserInCanvas(canvasId, userId);
    }

    /**
     * 列出Executions。
     */
    @Override
    public List<CanvasExecutionView> listExecutions(Long canvasId, String userId) {
        return catalog.listExecutions(canvasId, userId);
    }

    /**
     * 处理registerUser。
     */
    @Override
    public void registerUser(Long canvasId, CanvasUserCommand command) {
        catalog.registerUser(canvasId, command);
    }

    /**
     * 处理registerExecution。
     */
    @Override
    public void registerExecution(Long canvasId, String userId, ExecutionCommand command) {
        catalog.registerExecution(canvasId, userId, command);
    }
}
