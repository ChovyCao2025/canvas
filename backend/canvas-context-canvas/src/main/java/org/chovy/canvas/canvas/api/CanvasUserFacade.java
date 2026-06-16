package org.chovy.canvas.canvas.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 定义CanvasUserFacade对外提供的能力契约。
 */
public interface CanvasUserFacade {

    /**
     * 列出Users。
     */
    List<CanvasUserView> listUsers(Long canvasId);

    /**
     * 获取UserInCanvas。
     */
    CanvasUserView getUserInCanvas(Long canvasId, String userId);

    /**
     * 列出Executions。
     */
    List<CanvasExecutionView> listExecutions(Long canvasId, String userId);

    /**
     * 处理registerUser。
     */
    default void registerUser(Long canvasId, CanvasUserCommand command) {
    }

    /**
     * 处理registerExecution。
     */
    default void registerExecution(Long canvasId, String userId, ExecutionCommand command) {
    }

    /**
     * 承载CanvasUserCommand的数据快照。
     */
    record CanvasUserCommand(
            /**
             * 记录用户标识。
             */
            String userId,
            /**
             * 记录email。
             */
            String email,
            /**
             * 记录mobile。
             */
            String mobile,
            /**
             * 记录touchStatus。
             */
            String touchStatus,
            /**
             * 记录profile。
             */
            Map<String, Object> profile) {
    }

    /**
     * 承载ExecutionCommand的数据快照。
     */
    record ExecutionCommand(Long nodeId, String nodeKey, String status, LocalDateTime executedAt) {
    }

    /**
     * 承载CanvasUserView的数据快照。
     */
    record CanvasUserView(
            /**
             * 记录画布标识。
             */
            Long canvasId,
            /**
             * 记录用户标识。
             */
            String userId,
            /**
             * 记录email。
             */
            String email,
            /**
             * 记录mobile。
             */
            String mobile,
            /**
             * 记录touchStatus。
             */
            String touchStatus,
            /**
             * 记录profile。
             */
            Map<String, Object> profile) {
    }

    /**
     * 承载CanvasExecutionView的数据快照。
     */
    record CanvasExecutionView(
            /**
             * 记录标识。
             */
            Long id,
            /**
             * 记录画布标识。
             */
            Long canvasId,
            /**
             * 记录用户标识。
             */
            String userId,
            /**
             * 记录节点标识。
             */
            Long nodeId,
            /**
             * 记录nodeKey。
             */
            String nodeKey,
            /**
             * 记录状态。
             */
            String status,
            /**
             * 记录executed时间。
             */
            LocalDateTime executedAt) {
    }
}
