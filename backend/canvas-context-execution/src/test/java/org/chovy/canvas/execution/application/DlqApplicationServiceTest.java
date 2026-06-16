package org.chovy.canvas.execution.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.chovy.canvas.execution.api.DlqFacade;
import org.junit.jupiter.api.Test;

/**
 * 定义 DlqApplicationServiceTest 的执行上下文数据结构或业务契约。
 */
class DlqApplicationServiceTest {

    /**
     * 执行 listsWithCanvasFilterAndOneBasedPaging 对应的业务处理。
     */
    @Test
    void listsWithCanvasFilterAndOneBasedPaging() {
        DlqFacade service = new DlqApplicationService();

        DlqFacade.DlqPageView page = service.list(new DlqFacade.DlqQuery(42L, 1, 20));

        assertThat(page.total()).isEqualTo(2);
        assertThat(page.list()).extracting(DlqFacade.DlqEntryView::id)
                .containsExactly(1002L, 1001L);
        assertThat(page.list()).first()
                .returns(42L, DlqFacade.DlqEntryView::canvasId)
                .returns("user-2", DlqFacade.DlqEntryView::userId)
                .returns("MQ_EVENT", DlqFacade.DlqEntryView::triggerType)
                .returns("MESSAGE", DlqFacade.DlqEntryView::triggerNodeType)
                .returns("signup", DlqFacade.DlqEntryView::matchKey);

        DlqFacade.DlqPageView normalized = service.list(new DlqFacade.DlqQuery(null, -1, 500));

        assertThat(normalized.page()).isEqualTo(1);
        assertThat(normalized.size()).isEqualTo(100);
        assertThat(normalized.total()).isEqualTo(3);
    }

    /**
     * 执行 replayUsesOriginalDeadLetterMetadataAndKeepsEntryForExplicitDelete 对应的业务处理。
     */
    @Test
    void replayUsesOriginalDeadLetterMetadataAndKeepsEntryForExplicitDelete() {
        DlqFacade service = new DlqApplicationService();

        DlqFacade.DlqReplayResult replay = service.replay(1001L, true);

        assertThat(replay)
                .returns(1001L, DlqFacade.DlqReplayResult::dlqId)
                .returns(42L, DlqFacade.DlqReplayResult::canvasId)
                .returns("user-1", DlqFacade.DlqReplayResult::userId)
                .returns("DIRECT_CALL", DlqFacade.DlqReplayResult::triggerType)
                .returns("DIRECT_CALL", DlqFacade.DlqReplayResult::triggerNodeType)
                .returns("manual", DlqFacade.DlqReplayResult::matchKey)
                .returns(true, DlqFacade.DlqReplayResult::skipSuccessNodes);
        assertThat(replay.payload()).containsEntry("couponCode", "A10");
        assertThat(replay.replayId()).startsWith("dlq-replay-1001-");
        assertThat(service.list(new DlqFacade.DlqQuery(42L, 1, 20)).list())
                .extracting(DlqFacade.DlqEntryView::id)
                .contains(1001L);
    }

    /**
     * 执行 deleteRemovesDeadLetterAndMissingReplayFollowsCompatibilityMessage 对应的业务处理。
     */
    @Test
    void deleteRemovesDeadLetterAndMissingReplayFollowsCompatibilityMessage() {
        DlqFacade service = new DlqApplicationService();

        DlqFacade.DeleteResult deleted = service.delete(1002L);

        assertThat(deleted.deleted()).isTrue();
        assertThat(deleted.id()).isEqualTo(1002L);
        assertThat(service.list(new DlqFacade.DlqQuery(42L, 1, 20)).list())
                .extracting(DlqFacade.DlqEntryView::id)
                .doesNotContain(1002L);
        assertThatThrownBy(() -> service.replay(1002L, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DLQ 记录不存在: 1002");
    }

    /**
     * 执行 supportsRegisteringAdditionalDeadLettersForCompatibilityFixtures 对应的业务处理。
     */
    @Test
    void supportsRegisteringAdditionalDeadLettersForCompatibilityFixtures() {
        DlqFacade service = new DlqApplicationService();

        service.register(new DlqFacade.DlqEntryCommand(2001L, 77L, "user-77", "WAIT_EVENT",
                "WAIT", "order-paid", Map.of("orderId", "O-1"), "timeout"));

        DlqFacade.DlqPageView page = service.list(new DlqFacade.DlqQuery(77L, 1, 10));

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.list()).singleElement()
                .returns(2001L, DlqFacade.DlqEntryView::id)
                .returns("timeout", DlqFacade.DlqEntryView::errorMessage);
    }
}
