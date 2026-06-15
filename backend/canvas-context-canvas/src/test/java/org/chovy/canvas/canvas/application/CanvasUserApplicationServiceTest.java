package org.chovy.canvas.canvas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.canvas.api.CanvasUserFacade;
import org.junit.jupiter.api.Test;

class CanvasUserApplicationServiceTest {

    @Test
    void listsGetsAndTracesUsersWithinCanvasScope() {
        CanvasUserFacade service = new CanvasUserApplicationService();
        service.registerUser(100L, new CanvasUserFacade.CanvasUserCommand("user-1", "buyer@example.com",
                "13800000000", "ENTERED", Map.of("tier", "gold")));
        service.registerUser(100L, new CanvasUserFacade.CanvasUserCommand("user-2", null,
                null, "WAITING", Map.of()));
        service.registerExecution(100L, "user-1", new CanvasUserFacade.ExecutionCommand(11L, "send_sms",
                "SUCCESS", LocalDateTime.parse("2026-06-14T10:00:00")));
        service.registerUser(200L, new CanvasUserFacade.CanvasUserCommand("user-1", "other@example.com",
                null, "ENTERED", Map.of()));

        assertThat(service.listUsers(100L)).extracting(CanvasUserFacade.CanvasUserView::userId)
                .containsExactly("user-1", "user-2");
        assertThat(service.getUserInCanvas(100L, "user-1"))
                .returns(100L, CanvasUserFacade.CanvasUserView::canvasId)
                .returns("buyer@example.com", CanvasUserFacade.CanvasUserView::email)
                .returns("ENTERED", CanvasUserFacade.CanvasUserView::touchStatus);
        assertThat(service.getUserInCanvas(100L, "user-1").profile()).containsEntry("tier", "gold");
        assertThat(service.getUserInCanvas(200L, "user-1").email()).isEqualTo("other@example.com");
        assertThat(service.listExecutions(100L, "user-1")).singleElement()
                .returns(11L, CanvasUserFacade.CanvasExecutionView::nodeId)
                .returns("send_sms", CanvasUserFacade.CanvasExecutionView::nodeKey)
                .returns("SUCCESS", CanvasUserFacade.CanvasExecutionView::status);
    }

    @Test
    void validationAndMissingRowsFollowCompatibilityContract() {
        CanvasUserFacade service = new CanvasUserApplicationService();

        assertThatThrownBy(() -> service.listUsers(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("canvas id is required");
        assertThatThrownBy(() -> service.getUserInCanvas(1L, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user id is required");
        assertThatThrownBy(() -> service.getUserInCanvas(1L, "missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("canvas user is not found");
        assertThat(service.listExecutions(1L, "missing")).isEmpty();
    }
}
