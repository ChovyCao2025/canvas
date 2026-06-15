package org.chovy.canvas.canvas.api;

import java.util.List;

public interface CanvasCollaborationFacade {

    Summary summary(Long tenantId, Long canvasId);

    record Summary(Long canvasId,
                   List<Presence> presence,
                   int activeLockCount,
                   int openCommentCount,
                   int unreadNotificationCount) {
        public Summary {
            presence = presence == null ? List.of() : List.copyOf(presence);
        }
    }

    record Presence(String userId, String displayName, String state) {
    }
}
