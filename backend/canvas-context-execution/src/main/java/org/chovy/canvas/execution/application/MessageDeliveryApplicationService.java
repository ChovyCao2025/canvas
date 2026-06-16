package org.chovy.canvas.execution.application;

import java.util.List;
import java.util.Optional;

import org.chovy.canvas.execution.api.MessageDeliveryFacade;
import org.chovy.canvas.execution.domain.MessageDeliveryCatalog;
import org.springframework.stereotype.Service;

@Service
public class MessageDeliveryApplicationService implements MessageDeliveryFacade {

    private final MessageDeliveryCatalog catalog;

    public MessageDeliveryApplicationService() {
        this(new MessageDeliveryCatalog());
    }

    public MessageDeliveryApplicationService(MessageDeliveryCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public DeliveryPageView search(DeliverySearchQuery query) {
        return catalog.search(query);
    }

    @Override
    public Optional<MessageDeliveryCatalog.Delivery> findById(Long id) {
        return catalog.findById(id);
    }

    @Override
    public List<MessageDeliveryCatalog.Receipt> receipts(Long outboxId) {
        return catalog.receipts(outboxId);
    }

    @Override
    public ReplayResultView replay(Long id) {
        return catalog.replay(id);
    }

    @Override
    public ReconcileResultView reconcile() {
        return catalog.reconcile();
    }
}
