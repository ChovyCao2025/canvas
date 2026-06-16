package org.chovy.canvas.execution.api;

import java.util.List;
import java.util.Map;

public interface DlqFacade {

    DlqPageView list(DlqQuery query);

    DlqReplayResult replay(Long id, boolean skipSuccessNodes);

    DeleteResult delete(Long id);

    default void register(DlqEntryCommand command) {
    }

    record DlqQuery(Long canvasId, int page, int size) {
    }

    record DlqEntryCommand(
            Long id,
            Long canvasId,
            String userId,
            String triggerType,
            String triggerNodeType,
            String matchKey,
            Map<String, Object> payload,
            String errorMessage) {
    }

    record DlqPageView(long total, int page, int size, List<DlqEntryView> list) {
    }

    record DlqEntryView(
            Long id,
            Long canvasId,
            String userId,
            String triggerType,
            String triggerNodeType,
            String matchKey,
            Map<String, Object> payload,
            String errorMessage,
            String failedAt) {
    }

    record DlqReplayResult(
            Long dlqId,
            Long canvasId,
            String userId,
            String triggerType,
            String triggerNodeType,
            String matchKey,
            Map<String, Object> payload,
            boolean skipSuccessNodes,
            String replayId) {
    }

    record DeleteResult(Long id, boolean deleted) {
    }
}
