package org.chovy.canvas.execution.application;

import java.util.Optional;

import org.chovy.canvas.execution.api.MessageSendRecordFacade;
import org.chovy.canvas.execution.domain.MessageSendRecordCatalog;
import org.springframework.stereotype.Service;

@Service
public class MessageSendRecordApplicationService implements MessageSendRecordFacade {

    private final MessageSendRecordCatalog catalog;

    public MessageSendRecordApplicationService() {
        this(new MessageSendRecordCatalog());
    }

    public MessageSendRecordApplicationService(MessageSendRecordCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public MessageSendRecordPageView search(MessageSendRecordQuery query) {
        return catalog.search(query);
    }

    @Override
    public Optional<MessageSendRecordCatalog.MessageSendRecord> findById(Long id) {
        return catalog.findById(id);
    }
}
