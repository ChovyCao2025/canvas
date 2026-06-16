package org.chovy.canvas.canvas.api;

import java.util.List;

/**
 * 定义CanvasCollaborationFacade对外提供的能力契约。
 */
public interface CanvasCollaborationFacade {

    /**
     * 处理summary。
     */
    Summary summary(Long tenantId, Long canvasId);

    /**
     * 承载Summary的数据快照。
     */
    record Summary(Long canvasId,
                   /**
                    * 记录presence。
                    */
                   List<Presence> presence,
                   /**
                    * 记录activeLockCount。
                    */
                   int activeLockCount,
                   /**
                    * 记录openCommentCount。
                    */
                   int openCommentCount,
                   /**
                    * 记录unreadNotificationCount。
                    */
                   int unreadNotificationCount) {
        public Summary {
            presence = presence == null ? List.of() : List.copyOf(presence);
        }
    }

    /**
     * 承载Presence的数据快照。
     */
    record Presence(String userId, String displayName, String state) {
    }
}
