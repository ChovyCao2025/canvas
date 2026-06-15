package org.chovy.canvas.execution.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.chovy.canvas.execution.domain.MessageSendRecordCatalog;

public interface MessageSendRecordFacade {

    MessageSendRecordPageView search(MessageSendRecordQuery query);

    Optional<MessageSendRecordCatalog.MessageSendRecord> findById(Long id);

    record MessageSendRecordQuery(
            Long canvasId,
            String executionId,
            String userId,
            String channel,
            String status,
            LocalDateTime startAt,
            LocalDateTime endAt,
            int page,
            int size) {
        public MessageSendRecordQuery {
            page = Math.max(1, page);
            size = Math.max(1, Math.min(size, 100));
        }
    }

    record MessageSendRecordPageView(long total, List<MessageSendRecordCatalog.MessageSendRecord> list) {
        public MessageSendRecordPageView {
            list = List.copyOf(list == null ? List.of() : list);
        }
    }
}
