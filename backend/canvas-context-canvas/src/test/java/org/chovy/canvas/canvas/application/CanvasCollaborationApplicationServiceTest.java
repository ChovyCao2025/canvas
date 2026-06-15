package org.chovy.canvas.canvas.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.chovy.canvas.canvas.api.CanvasCollaborationFacade;
import org.junit.jupiter.api.Test;

class CanvasCollaborationApplicationServiceTest {

    @Test
    void returnsLegacyDefaultSummaryWhenRepositoryHasNoSummary() {
        CanvasCollaborationFacade service = new CanvasCollaborationApplicationService();

        CanvasCollaborationFacade.Summary summary = service.summary(7L, 42L);

        assertThat(summary.canvasId()).isEqualTo(42L);
        assertThat(summary.presence()).isEmpty();
        assertThat(summary.activeLockCount()).isZero();
        assertThat(summary.openCommentCount()).isZero();
        assertThat(summary.unreadNotificationCount()).isZero();
    }

    @Test
    void readsSummaryByTenantAndCanvas() {
        CanvasCollaborationApplicationService service = new CanvasCollaborationApplicationService((tenantId, canvasId) -> {
            if (tenantId == 7L && canvasId == 42L) {
                return new CanvasCollaborationFacade.Summary(
                        canvasId,
                        List.of(new CanvasCollaborationFacade.Presence("user-1", "Operator One", "online")),
                        2,
                        3,
                        4);
            }
            return null;
        });

        CanvasCollaborationFacade.Summary seeded = service.summary(7L, 42L);
        CanvasCollaborationFacade.Summary otherTenant = service.summary(8L, 42L);

        assertThat(seeded.presence())
                .singleElement()
                .satisfies(presence -> assertThat(presence)
                        .returns("user-1", CanvasCollaborationFacade.Presence::userId)
                        .returns("Operator One", CanvasCollaborationFacade.Presence::displayName)
                        .returns("online", CanvasCollaborationFacade.Presence::state));
        assertThat(seeded.activeLockCount()).isEqualTo(2);
        assertThat(seeded.openCommentCount()).isEqualTo(3);
        assertThat(seeded.unreadNotificationCount()).isEqualTo(4);
        assertThat(otherTenant.presence()).isEmpty();
    }
}
