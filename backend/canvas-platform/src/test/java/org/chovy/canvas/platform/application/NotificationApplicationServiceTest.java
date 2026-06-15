package org.chovy.canvas.platform.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.api.NotificationFacade;
import org.junit.jupiter.api.Test;

class NotificationApplicationServiceTest {

    @Test
    void listAppliesTenantUserFiltersArchivedUnreadAndPageSizeBounds() {
        NotificationFacade service = new NotificationApplicationService();

        List<Map<String, Object>> unreadTaskRows = service.list(7L, "operator-1", true, false,
                "TASK", 0, 0);
        List<Map<String, Object>> archivedRows = service.list(7L, "operator-1", false, true,
                null, 1, 100);
        List<Map<String, Object>> otherTenantRows = service.list(8L, "operator-1", false, false,
                null, 1, 100);

        assertThat(unreadTaskRows).hasSize(1);
        assertThat(unreadTaskRows.get(0))
                .containsEntry("notificationId", "ntf_task_failed_001")
                .containsEntry("type", "TASK_FAILED")
                .containsEntry("category", "TASK")
                .containsEntry("severity", "ERROR")
                .containsEntry("status", "UNREAD")
                .containsEntry("title", "Import failed")
                .containsEntry("taskId", "task-1001")
                .containsEntry("bizType", "ASYNC_TASK")
                .containsEntry("payloadJson", "{\"taskId\":\"task-1001\"}");
        assertThat(unreadTaskRows.get(0)).containsKeys("content", "targetUrl", "actionLabel", "actionUrl",
                "bizId", "dedupKey", "readAt", "archivedAt", "deliveredAt", "createdAt");
        assertThat(archivedRows)
                .extracting(row -> row.get("notificationId"))
                .containsExactly("ntf_archived_001");
        assertThat(otherTenantRows)
                .extracting(row -> row.get("notificationId"))
                .containsExactly("ntf_tenant8_001");
    }

    @Test
    void readAndArchiveMutationsAreScopedAndUpdateUnreadCount() {
        NotificationFacade service = new NotificationApplicationService();

        assertThat(service.unreadCount(7L, "operator-1")).containsEntry("count", 2L);

        service.markRead(7L, "operator-1", "ntf_task_failed_001");
        service.markRead(8L, "operator-1", "ntf_canvas_001");

        assertThat(service.unreadCount(7L, "operator-1")).containsEntry("count", 1L);
        assertThat(service.unreadCount(8L, "operator-1")).containsEntry("count", 1L);

        service.archive(7L, "operator-1", "ntf_canvas_001");

        assertThat(service.unreadCount(7L, "operator-1")).containsEntry("count", 0L);
        assertThat(service.list(7L, "operator-1", false, true, null, 1, 20))
                .extracting(row -> row.get("notificationId"))
                .containsExactly("ntf_archived_001", "ntf_canvas_001");
    }

    @Test
    void markAllReadKeepsArchivedRowsOutOfUnreadCountAndIsIdempotent() {
        NotificationFacade service = new NotificationApplicationService();

        service.markAllRead(7L, "operator-1");
        service.markAllRead(7L, "operator-1");

        assertThat(service.unreadCount(7L, "operator-1")).containsEntry("count", 0L);
        assertThat(service.list(7L, "operator-1", false, false, null, 1, 20))
                .extracting(row -> row.get("status"))
                .containsExactly("READ", "READ");
    }

    @Test
    void websocketTicketPreservesLegacyShapeAndInvalidIdsFailFast() {
        NotificationFacade service = new NotificationApplicationService();

        Map<String, Object> ticket = service.createWsTicket(7L, "operator-1");

        assertThat(ticket)
                .containsEntry("expiresInSeconds", 60);
        assertThat(ticket.get("ticket")).asString().startsWith("ntf_ws_");

        assertThatThrownBy(() -> service.markRead(7L, "operator-1", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("notificationId is required");
    }
}
