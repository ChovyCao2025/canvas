package org.chovy.canvas.platform.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.api.NotificationFacade;
import org.chovy.canvas.platform.domain.NotificationCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationApplicationService implements NotificationFacade {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "system";

    private final NotificationCatalog catalog;

    public NotificationApplicationService() {
        this(new NotificationCatalog());
    }

    public NotificationApplicationService(NotificationCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public List<Map<String, Object>> list(Long tenantId, String actor, boolean unreadOnly, boolean archived,
                                          String category, int page, int size) {
        return catalog.list(safeTenantId(tenantId), actorOrDefault(actor), unreadOnly, archived, blankToNull(category),
                safePage(page), safeSize(size));
    }

    @Override
    public Map<String, Object> unreadCount(Long tenantId, String actor) {
        return catalog.unreadCount(safeTenantId(tenantId), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long tenantId, String actor, String notificationId) {
        catalog.markRead(safeTenantId(tenantId), actorOrDefault(actor), requireNotificationId(notificationId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long tenantId, String actor) {
        catalog.markAllRead(safeTenantId(tenantId), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archive(Long tenantId, String actor, String notificationId) {
        catalog.archive(safeTenantId(tenantId), actorOrDefault(actor), requireNotificationId(notificationId));
    }

    @Override
    public Map<String, Object> createWsTicket(Long tenantId, String actor) {
        return catalog.createWsTicket(safeTenantId(tenantId), actorOrDefault(actor));
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId <= 0 ? DEFAULT_TENANT_ID : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    private static int safePage(int page) {
        return Math.max(1, page);
    }

    private static int safeSize(int size) {
        return Math.max(1, Math.min(100, size));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String requireNotificationId(String notificationId) {
        if (notificationId == null || notificationId.isBlank()) {
            throw new IllegalArgumentException("notificationId is required");
        }
        return notificationId.trim();
    }
}
