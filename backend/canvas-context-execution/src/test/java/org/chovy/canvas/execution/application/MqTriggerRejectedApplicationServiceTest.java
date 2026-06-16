package org.chovy.canvas.execution.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.execution.api.MqTriggerRejectedFacade;
import org.junit.jupiter.api.Test;

/**
 * 定义 MqTriggerRejectedApplicationServiceTest 的执行上下文数据结构或业务契约。
 */
class MqTriggerRejectedApplicationServiceTest {

    /**
     * 执行 listsWithTagReasonFiltersAndPaging 对应的业务处理。
     */
    @Test
    void listsWithTagReasonFiltersAndPaging() {
        MqTriggerRejectedFacade service = new MqTriggerRejectedApplicationService();

        MqTriggerRejectedFacade.RejectedPageView page = service.list(new MqTriggerRejectedFacade.RejectedQuery(
                "signup-topic", "NO_ROUTE", 1, 20));

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.list()).singleElement()
                .returns(1001L, MqTriggerRejectedFacade.RejectedView::id)
                .returns("signup-topic", MqTriggerRejectedFacade.RejectedView::tag)
                .returns("msg-1", MqTriggerRejectedFacade.RejectedView::msgId)
                .returns("NO_ROUTE", MqTriggerRejectedFacade.RejectedView::reason);

        MqTriggerRejectedFacade.RejectedPageView normalized = service.list(new MqTriggerRejectedFacade.RejectedQuery(
                null, null, 0, 500));

        assertThat(normalized.page()).isEqualTo(1);
        assertThat(normalized.size()).isEqualTo(100);
        assertThat(normalized.total()).isEqualTo(3);
    }

    /**
     * 执行 detailReturnsRecordOrCompatibilityMissingMessage 对应的业务处理。
     */
    @Test
    void detailReturnsRecordOrCompatibilityMissingMessage() {
        MqTriggerRejectedFacade service = new MqTriggerRejectedApplicationService();

        assertThat(service.detail(1002L))
                .returns(1002L, MqTriggerRejectedFacade.RejectedView::id)
                .returns("payment-topic", MqTriggerRejectedFacade.RejectedView::tag);
        assertThatThrownBy(() -> service.detail(9999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rejected 消息不存在: 9999");
    }

    /**
     * 执行 replayParsesMessageRoutesByCurrentTagAndReportsDispatchFailures 对应的业务处理。
     */
    @Test
    void replayParsesMessageRoutesByCurrentTagAndReportsDispatchFailures() {
        MqTriggerRejectedFacade service = new MqTriggerRejectedApplicationService();

        MqTriggerRejectedFacade.ReplayResult replay = service.replay(1002L);

        assertThat(replay.count()).isEqualTo(2);
        assertThat(replay.requestIds()).containsExactly("mq-replay-42-msg-2", "mq-replay-43-msg-2");
        assertThat(replay.dispatchFailureCount()).isEqualTo(1);
        assertThat(replay.dispatchFailedRequestIds()).containsExactly("mq-replay-43-msg-2");
    }

    /**
     * 执行 replayRejectsMalformedOrIncompleteMessageBodies 对应的业务处理。
     */
    @Test
    void replayRejectsMalformedOrIncompleteMessageBodies() {
        MqTriggerRejectedFacade service = new MqTriggerRejectedApplicationService();

        assertThatThrownBy(() -> service.replay(1003L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("消息体不是合法 MQ 触发 JSON");

        service.register(new MqTriggerRejectedFacade.RejectedCommand(2001L, "broken-topic", "msg-x",
                "INVALID_BODY", Map.of("messageCode", "signup.created"), List.of("42")));

        assertThatThrownBy(() -> service.replay(2001L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("缺少 userId/messageCode/payload");
    }

    /**
     * 执行 replaySkipsInvalidRoutesAndCanReturnZeroRequests 对应的业务处理。
     */
    @Test
    void replaySkipsInvalidRoutesAndCanReturnZeroRequests() {
        MqTriggerRejectedFacade service = new MqTriggerRejectedApplicationService();
        service.register(new MqTriggerRejectedFacade.RejectedCommand(2002L, "dirty-topic", "msg-dirty",
                "NO_VALID_ROUTE", Map.of(
                        "userId", "user-9",
                        "messageCode", "dirty.created",
                        "payload", Map.of("source", "test")),
                List.of("-1", "abc")));

        MqTriggerRejectedFacade.ReplayResult replay = service.replay(2002L);

        assertThat(replay.count()).isZero();
        assertThat(replay.requestIds()).isEmpty();
        assertThat(replay.dispatchFailureCount()).isZero();
    }
}
