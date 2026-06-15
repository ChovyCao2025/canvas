package org.chovy.canvas.platform.api;

import java.util.List;
import java.util.Map;

public interface NotificationFacade {

    List<Map<String, Object>> list(Long tenantId, String actor, boolean unreadOnly, boolean archived, String category,
                                   int page, int size);

    Map<String, Object> unreadCount(Long tenantId, String actor);

    void markRead(Long tenantId, String actor, String notificationId);

    void markAllRead(Long tenantId, String actor);

    void archive(Long tenantId, String actor, String notificationId);

    Map<String, Object> createWsTicket(Long tenantId, String actor);
}
