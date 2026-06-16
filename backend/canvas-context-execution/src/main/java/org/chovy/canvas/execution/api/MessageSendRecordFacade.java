package org.chovy.canvas.execution.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.chovy.canvas.execution.domain.MessageSendRecordCatalog;

/**
 * 定义 MessageSendRecordFacade 的执行上下文数据结构或业务契约。
 */
public interface MessageSendRecordFacade {

    /**
     * 执行 search 对应的业务处理。
     * @param query query 参数
     * @return 处理后的结果
     */
    MessageSendRecordPageView search(MessageSendRecordQuery query);

    /**
     * 执行 findById 对应的业务处理。
     * @param id id 参数
     * @return 处理后的结果
     */
    Optional<MessageSendRecordCatalog.MessageSendRecord> findById(Long id);

    /**
     * 定义 MessageSendRecordQuery 的执行上下文数据结构或业务契约。
     * @param canvasId canvasId 对应的数据字段
     * @param executionId executionId 对应的数据字段
     * @param userId userId 对应的数据字段
     * @param channel channel 对应的数据字段
     * @param status status 对应的数据字段
     * @param startAt startAt 对应的数据字段
     * @param endAt endAt 对应的数据字段
     * @param page page 对应的数据字段
     * @param size size 对应的数据字段
     */
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

    /**
     * 定义 MessageSendRecordPageView 的执行上下文数据结构或业务契约。
     * @param total total 对应的数据字段
     * @param list list 对应的数据字段
     */
    record MessageSendRecordPageView(long total, List<MessageSendRecordCatalog.MessageSendRecord> list) {
        public MessageSendRecordPageView {
            list = List.copyOf(list == null ? List.of() : list);
        }
    }
}
