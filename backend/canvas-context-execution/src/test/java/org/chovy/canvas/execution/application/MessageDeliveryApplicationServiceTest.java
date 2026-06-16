package org.chovy.canvas.execution.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.chovy.canvas.execution.api.MessageDeliveryFacade.DeliveryPageView;
import org.chovy.canvas.execution.api.MessageDeliveryFacade.DeliverySearchQuery;
import org.chovy.canvas.execution.api.MessageDeliveryFacade.ReconcileResultView;
import org.chovy.canvas.execution.api.MessageDeliveryFacade.ReplayResultView;
import org.chovy.canvas.execution.domain.MessageDeliveryCatalog;
import org.junit.jupiter.api.Test;

/**
 * 定义 MessageDeliveryApplicationServiceTest 的执行上下文数据结构或业务契约。
 */
class MessageDeliveryApplicationServiceTest {

    /**
     * 执行 searchesWithLegacyFiltersAndOneBasedPaging 对应的业务处理。
     */
    @Test
    void searchesWithLegacyFiltersAndOneBasedPaging() {
        MessageDeliveryApplicationService service = new MessageDeliveryApplicationService();

        DeliveryPageView page = service.search(new DeliverySearchQuery(
                7L,
                42L,
                "exec-1",
                "user-1",
                "SMS",
                "twilio",
                "DEAD",
                "pm-dead-1",
                1,
                20));

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.list()).singleElement()
                .satisfies(delivery -> {
                    assertThat(delivery.id()).isEqualTo(1002L);
                    assertThat(delivery.tenantId()).isEqualTo(7L);
                    assertThat(delivery.canvasId()).isEqualTo(42L);
                    assertThat(delivery.executionId()).isEqualTo("exec-1");
                    assertThat(delivery.userId()).isEqualTo("user-1");
                    assertThat(delivery.channel()).isEqualTo("SMS");
                    assertThat(delivery.provider()).isEqualTo("twilio");
                    assertThat(delivery.status()).isEqualTo("DEAD");
                    assertThat(delivery.providerMessageId()).isEqualTo("pm-dead-1");
                });

        DeliveryPageView secondPage = service.search(new DeliverySearchQuery(
                7L, null, null, null, null, null, null, null, 2, 1));

        assertThat(secondPage.total()).isEqualTo(2);
        assertThat(secondPage.list()).extracting(MessageDeliveryCatalog.Delivery::id)
                .containsExactly(1001L);
    }

    /**
     * 执行 exposesDetailReceiptReplayAndReconcileSemantics 对应的业务处理。
     */
    @Test
    void exposesDetailReceiptReplayAndReconcileSemantics() {
        MessageDeliveryApplicationService service = new MessageDeliveryApplicationService();

        assertThat(service.findById(1002L)).isPresent();
        assertThat(service.findById(9999L)).isEmpty();

        assertThat(service.receipts(1001L))
                .extracting(MessageDeliveryCatalog.Receipt::receivedAt)
                .containsExactly(
                        LocalDateTime.parse("2026-06-12T10:03:00"),
                        LocalDateTime.parse("2026-06-12T10:02:00"));

        ReplayResultView replayed = service.replay(1002L);
        assertThat(replayed.replayed()).isTrue();
        assertThat(replayed.outboxId()).isEqualTo(1002L);
        assertThat(replayed.status()).isEqualTo("PENDING");

        ReplayResultView notReplayable = service.replay(1001L);
        assertThat(notReplayable.replayed()).isFalse();
        assertThat(notReplayable.outboxId()).isEqualTo(1001L);

        ReconcileResultView reconciled = service.reconcile();
        assertThat(reconciled.requeued()).isEqualTo(1);
        assertThat(service.search(new DeliverySearchQuery(
                7L, null, null, null, null, null, "PENDING", null, 1, 20)).list())
                .extracting(MessageDeliveryCatalog.Delivery::id)
                .contains(1002L);
    }
}
