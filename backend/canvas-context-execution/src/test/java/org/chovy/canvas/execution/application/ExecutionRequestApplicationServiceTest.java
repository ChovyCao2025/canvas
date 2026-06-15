package org.chovy.canvas.execution.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.chovy.canvas.execution.api.ExecutionRequestFacade;
import org.junit.jupiter.api.Test;

class ExecutionRequestApplicationServiceTest {

    @Test
    void listsWithFiltersAndOneBasedPaging() {
        ExecutionRequestFacade service = new ExecutionRequestApplicationService();

        ExecutionRequestFacade.RequestPageView page = service.list(new ExecutionRequestFacade.RequestQuery(
                7L, 42L, "FAILED", "user-1", "msg-1", 1, 20));

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.list()).singleElement()
                .returns("req-1", ExecutionRequestFacade.RequestView::id)
                .returns(7L, ExecutionRequestFacade.RequestView::tenantId)
                .returns(42L, ExecutionRequestFacade.RequestView::canvasId)
                .returns("FAILED", ExecutionRequestFacade.RequestView::status)
                .returns("user-1", ExecutionRequestFacade.RequestView::userId)
                .returns("msg-1", ExecutionRequestFacade.RequestView::sourceMsgId);

        ExecutionRequestFacade.RequestPageView normalized = service.list(new ExecutionRequestFacade.RequestQuery(
                7L, null, null, null, null, 0, 500));

        assertThat(normalized.page()).isEqualTo(1);
        assertThat(normalized.size()).isEqualTo(100);
        assertThat(normalized.total()).isEqualTo(3);
    }

    @Test
    void singleReplayOnlyAllowsFailedOrRetryWithoutForceAndQueuesRequest() {
        ExecutionRequestFacade service = new ExecutionRequestApplicationService();

        ExecutionRequestFacade.ReplayResult replay = service.replay("req-1",
                new ExecutionRequestFacade.ReplayCommand(7L, "operator-1", "manual retry", false));

        assertThat(replay.requestId()).isEqualTo("req-1");
        assertThat(replay.status()).isEqualTo("QUEUED");
        assertThat(replay.immediateDispatch()).isTrue();
        assertThat(service.list(new ExecutionRequestFacade.RequestQuery(7L, 42L, "PENDING",
                "user-1", null, 1, 20)).list())
                .extracting(ExecutionRequestFacade.RequestView::id)
                .contains("req-1");

        assertThatThrownBy(() -> service.replay("req-3",
                new ExecutionRequestFacade.ReplayCommand(7L, "operator-1", null, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("只能重放 FAILED/RETRY 状态的执行请求");
    }

    @Test
    void forceReplayBypassesStatusGuardAndMissingTenantAccessIsRejected() {
        ExecutionRequestFacade service = new ExecutionRequestApplicationService();

        assertThat(service.replay("req-3",
                new ExecutionRequestFacade.ReplayCommand(7L, "operator-1", "force", true)).status())
                .isEqualTo("QUEUED");

        assertThatThrownBy(() -> service.replay("req-4",
                new ExecutionRequestFacade.ReplayCommand(7L, "operator-1", "wrong tenant", true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("跨租户执行请求访问被拒绝");
        assertThatThrownBy(() -> service.replay("missing",
                new ExecutionRequestFacade.ReplayCommand(7L, "operator-1", null, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("执行请求不存在: missing");
    }

    @Test
    void batchReplayDefaultsToFailedAndRetryAndNormalizesLimit() {
        ExecutionRequestFacade service = new ExecutionRequestApplicationService();

        ExecutionRequestFacade.BatchReplayResult result = service.replayBatch(new ExecutionRequestFacade.BatchReplayCommand(
                7L, 42L, null, null, null, 0, "batch", false));

        assertThat(result.limit()).isEqualTo(100);
        assertThat(result.count()).isEqualTo(2);
        assertThat(result.requestIds()).containsExactly("req-2", "req-1");
        assertThat(result.dispatchFailureCount()).isZero();
    }

    @Test
    void batchReplayRejectsNonReplayableExplicitStatusUnlessForced() {
        ExecutionRequestFacade service = new ExecutionRequestApplicationService();

        assertThatThrownBy(() -> service.replayBatch(new ExecutionRequestFacade.BatchReplayCommand(
                7L, 42L, "SUCCESS", null, null, 10, "bad batch", false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("批量重放默认只允许 FAILED/RETRY");

        ExecutionRequestFacade.BatchReplayResult forced = service.replayBatch(new ExecutionRequestFacade.BatchReplayCommand(
                7L, 42L, "SUCCESS", null, null, 999, "force batch", true));

        assertThat(forced.limit()).isEqualTo(500);
        assertThat(forced.requestIds()).containsExactly("req-3");
    }

    @Test
    void supportsRegisteringAdditionalExecutionRequestsForCompatibilityFixtures() {
        ExecutionRequestFacade service = new ExecutionRequestApplicationService();

        service.register(new ExecutionRequestFacade.RequestCommand("req-new", 7L, 99L, "RETRY", "user-new",
                "msg-new", Map.of("source", "fixture")));

        assertThat(service.list(new ExecutionRequestFacade.RequestQuery(7L, 99L, "RETRY",
                "user-new", "msg-new", 1, 10)).list())
                .singleElement()
                .returns("req-new", ExecutionRequestFacade.RequestView::id);
    }
}
