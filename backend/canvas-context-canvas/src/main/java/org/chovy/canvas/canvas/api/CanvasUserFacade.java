package org.chovy.canvas.canvas.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface CanvasUserFacade {

    List<CanvasUserView> listUsers(Long canvasId);

    CanvasUserView getUserInCanvas(Long canvasId, String userId);

    List<CanvasExecutionView> listExecutions(Long canvasId, String userId);

    default void registerUser(Long canvasId, CanvasUserCommand command) {
    }

    default void registerExecution(Long canvasId, String userId, ExecutionCommand command) {
    }

    record CanvasUserCommand(
            String userId,
            String email,
            String mobile,
            String touchStatus,
            Map<String, Object> profile) {
    }

    record ExecutionCommand(Long nodeId, String nodeKey, String status, LocalDateTime executedAt) {
    }

    record CanvasUserView(
            Long canvasId,
            String userId,
            String email,
            String mobile,
            String touchStatus,
            Map<String, Object> profile) {
    }

    record CanvasExecutionView(
            Long id,
            Long canvasId,
            String userId,
            Long nodeId,
            String nodeKey,
            String status,
            LocalDateTime executedAt) {
    }
}
