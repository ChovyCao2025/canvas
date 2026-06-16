package org.chovy.canvas.execution.api;

import java.util.List;
import java.util.Optional;

import org.chovy.canvas.execution.domain.MessageDeliveryCatalog;

public interface MessageDeliveryFacade {

    DeliveryPageView search(DeliverySearchQuery query);

    Optional<MessageDeliveryCatalog.Delivery> findById(Long id);

    List<MessageDeliveryCatalog.Receipt> receipts(Long outboxId);

    ReplayResultView replay(Long id);

    ReconcileResultView reconcile();

    record DeliverySearchQuery(
            Long tenantId,
            Long canvasId,
            String executionId,
            String userId,
            String channel,
            String provider,
            String status,
            String providerMessageId,
            int page,
            int size) {
        public DeliverySearchQuery {
            page = Math.max(1, page);
            size = Math.max(1, Math.min(size, 100));
        }
    }

    record DeliveryPageView(long total, List<MessageDeliveryCatalog.Delivery> list) {
        public DeliveryPageView {
            list = List.copyOf(list == null ? List.of() : list);
        }
    }

    record ReplayResultView(Long outboxId, String status, boolean replayed) {
    }

    record ReconcileResultView(int requeued) {
    }
}
