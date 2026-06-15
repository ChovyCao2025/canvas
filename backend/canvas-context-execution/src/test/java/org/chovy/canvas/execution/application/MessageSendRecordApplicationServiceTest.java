package org.chovy.canvas.execution.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.chovy.canvas.execution.api.MessageSendRecordFacade.MessageSendRecordQuery;
import org.chovy.canvas.execution.domain.MessageSendRecordCatalog.MessageSendRecord;
import org.junit.jupiter.api.Test;

class MessageSendRecordApplicationServiceTest {

    @Test
    void searchesWithLegacyFiltersRangeSortingAndPagingBounds() {
        MessageSendRecordApplicationService service = new MessageSendRecordApplicationService();

        var page = service.search(new MessageSendRecordQuery(
                42L,
                "exec-1",
                "user-1",
                " sms ",
                " sent ",
                LocalDateTime.parse("2026-06-12T09:59:00"),
                LocalDateTime.parse("2026-06-12T10:10:00"),
                0,
                500));

        assertThat(page.total()).isEqualTo(2);
        assertThat(page.list()).extracting(MessageSendRecord::id)
                .containsExactly(502L, 501L);
        assertThat(page.list()).allSatisfy(record -> {
            assertThat(record.canvasId()).isEqualTo(42L);
            assertThat(record.executionId()).isEqualTo("exec-1");
            assertThat(record.userId()).isEqualTo("user-1");
            assertThat(record.channel()).isEqualTo("SMS");
            assertThat(record.status()).isEqualTo("SENT");
        });

        assertThat(service.search(new MessageSendRecordQuery(
                42L, null, null, null, null, null, null, 2, 1)).list())
                .extracting(MessageSendRecord::id)
                .containsExactly(501L);
    }

    @Test
    void detailReturnsRecordOrEmptyForLegacyFailureMapping() {
        MessageSendRecordApplicationService service = new MessageSendRecordApplicationService();

        assertThat(service.findById(501L)).isPresent()
                .get()
                .extracting(MessageSendRecord::externalMessageId)
                .isEqualTo("pm-501");
        assertThat(service.findById(9999L)).isEmpty();
    }
}
