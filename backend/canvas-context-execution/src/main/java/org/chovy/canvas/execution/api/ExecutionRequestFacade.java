package org.chovy.canvas.execution.api;

import java.util.List;
import java.util.Map;

public interface ExecutionRequestFacade {

    RequestPageView list(RequestQuery query);

    ReplayResult replay(String id, ReplayCommand command);

    BatchReplayResult replayBatch(BatchReplayCommand command);

    default void register(RequestCommand command) {
    }

    record RequestQuery(
            Long tenantId,
            Long canvasId,
            String status,
            String userId,
            String sourceMsgId,
            int page,
            int size) {
    }

    record ReplayCommand(Long tenantId, String operator, String reason, boolean force) {
    }

    record BatchReplayCommand(
            Long tenantId,
            Long canvasId,
            String status,
            String userId,
            String sourceMsgId,
            int limit,
            String reason,
            boolean force) {
    }

    record RequestCommand(
            String id,
            Long tenantId,
            Long canvasId,
            String status,
            String userId,
            String sourceMsgId,
            Map<String, Object> payload) {
    }

    record RequestPageView(long total, int page, int size, List<RequestView> list) {
    }

    record RequestView(
            String id,
            Long tenantId,
            Long canvasId,
            String status,
            String userId,
            String sourceMsgId,
            Map<String, Object> payload,
            String createdAt,
            String updatedAt) {
    }

    record ReplayResult(String requestId, String status, boolean immediateDispatch) {
    }

    record BatchReplayResult(
            int count,
            int limit,
            List<String> requestIds,
            int dispatchFailureCount,
            List<String> dispatchFailedRequestIds) {
    }
}
