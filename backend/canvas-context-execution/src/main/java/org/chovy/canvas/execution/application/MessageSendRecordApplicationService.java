package org.chovy.canvas.execution.application;

import java.util.Optional;

import org.chovy.canvas.execution.api.MessageSendRecordFacade;
import org.chovy.canvas.execution.domain.MessageSendRecordCatalog;
import org.springframework.stereotype.Service;

/**
 * 定义 MessageSendRecordApplicationService 的执行上下文数据结构或业务契约。
 */
@Service
public class MessageSendRecordApplicationService implements MessageSendRecordFacade {

    /**
     * 保存 catalog 对应的状态或配置。
     */
    private final MessageSendRecordCatalog catalog;

    /**
     * 执行 MessageSendRecordApplicationService 对应的业务处理。
     */
    public MessageSendRecordApplicationService() {
        this(new MessageSendRecordCatalog());
    }

    /**
     * 执行 MessageSendRecordApplicationService 对应的业务处理。
     * @param catalog catalog 参数
     */
    public MessageSendRecordApplicationService(MessageSendRecordCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 执行 search 对应的业务处理。
     * @param query query 参数
     * @return 处理后的结果
     */
    @Override
    public MessageSendRecordPageView search(MessageSendRecordQuery query) {
        return catalog.search(query);
    }

    /**
     * 执行 findById 对应的业务处理。
     * @param id id 参数
     * @return 处理后的结果
     */
    @Override
    public Optional<MessageSendRecordCatalog.MessageSendRecord> findById(Long id) {
        return catalog.findById(id);
    }
}
