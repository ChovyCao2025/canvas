package org.chovy.canvas.execution.api;

import java.util.List;
import java.util.Map;

public interface MqTriggerRejectedFacade {

    RejectedPageView list(RejectedQuery query);

    RejectedView detail(Long id);

    ReplayResult replay(Long id);

    default void register(RejectedCommand command) {
    }

    record RejectedQuery(String tag, String reason, int page, int size) {
    }

    record RejectedCommand(
            Long id,
            String tag,
            String msgId,
            String reason,
            Map<String, Object> body,
            List<String> routes) {
    }

    record RejectedPageView(long total, int page, int size, List<RejectedView> list) {
    }

    record RejectedView(
            Long id,
            String tag,
            String msgId,
            String reason,
            Map<String, Object> body,
            String createdAt) {
    }

    record ReplayResult(
            int count,
            List<String> requestIds,
            int dispatchFailureCount,
            List<String> dispatchFailedRequestIds) {
    }
}
