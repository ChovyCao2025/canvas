package org.chovy.canvas.platform.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.api.NotificationFacade;
import org.chovy.canvas.platform.domain.NotificationCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 通知应用服务，负责通知查询、已读归档和 WebSocket 票据创建。
 */
@Service
public class NotificationApplicationService implements NotificationFacade {

    /**
     * 通知接口缺省租户标识。
     */
    private static final Long DEFAULT_TENANT_ID = 7L;

    /**
     * 通知接口缺省操作者。
     */
    private static final String DEFAULT_ACTOR = "system";

    /**
     * 保存通知数据和票据数据的目录。
     */
    private final NotificationCatalog catalog;

    /**
     * 使用默认内存目录创建通知应用服务。
     */
    public NotificationApplicationService() {
        this(new NotificationCatalog());
    }

    /**
     * 使用指定目录创建通知应用服务。
     *
     * @param catalog 通知目录
     */
    public NotificationApplicationService(NotificationCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 分页查询通知。
     *
     * @param tenantId 租户标识
     * @param actor 操作者
     * @param unreadOnly 是否只查询未读通知
     * @param archived 是否查询已归档通知
     * @param category 通知分类
     * @param page 页码
     * @param size 每页数量
     * @return 通知列表
     */
    @Override
    public List<Map<String, Object>> list(Long tenantId, String actor, boolean unreadOnly, boolean archived,
                                          String category, int page, int size) {
        return catalog.list(safeTenantId(tenantId), actorOrDefault(actor), unreadOnly, archived, blankToNull(category),
                safePage(page), safeSize(size));
    }

    /**
     * 查询未读通知数量。
     *
     * @param tenantId 租户标识
     * @param actor 操作者
     * @return 未读数量记录
     */
    @Override
    public Map<String, Object> unreadCount(Long tenantId, String actor) {
        return catalog.unreadCount(safeTenantId(tenantId), actorOrDefault(actor));
    }

    /**
     * 标记单条通知为已读。
     *
     * @param tenantId 租户标识
     * @param actor 操作者
     * @param notificationId 通知标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long tenantId, String actor, String notificationId) {
        catalog.markRead(safeTenantId(tenantId), actorOrDefault(actor), requireNotificationId(notificationId));
    }

    /**
     * 标记全部通知为已读。
     *
     * @param tenantId 租户标识
     * @param actor 操作者
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long tenantId, String actor) {
        catalog.markAllRead(safeTenantId(tenantId), actorOrDefault(actor));
    }

    /**
     * 归档单条通知。
     *
     * @param tenantId 租户标识
     * @param actor 操作者
     * @param notificationId 通知标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archive(Long tenantId, String actor, String notificationId) {
        catalog.archive(safeTenantId(tenantId), actorOrDefault(actor), requireNotificationId(notificationId));
    }

    /**
     * 创建通知 WebSocket 票据。
     *
     * @param tenantId 租户标识
     * @param actor 操作者
     * @return WebSocket 票据记录
     */
    @Override
    public Map<String, Object> createWsTicket(Long tenantId, String actor) {
        return catalog.createWsTicket(safeTenantId(tenantId), actorOrDefault(actor));
    }

    /**
     * 将缺失或非法租户标识归一到通知演示租户。
     *
     * @param tenantId 原始租户标识
     * @return 可传递给目录层的租户标识
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId <= 0 ? DEFAULT_TENANT_ID : tenantId;
    }

    /**
     * 将缺失操作者归一为系统操作者。
     *
     * @param actor 原始操作者
     * @return 可审计的操作者名称
     */
    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    /**
     * 将页码归一为正数。
     *
     * @param page 原始页码
     * @return 不小于 1 的页码
     */
    private static int safePage(int page) {
        return Math.max(1, page);
    }

    /**
     * 将每页数量限制在通知接口允许范围。
     *
     * @param size 原始每页数量
     * @return 1 到 100 之间的每页数量
     */
    private static int safeSize(int size) {
        return Math.max(1, Math.min(100, size));
    }

    /**
     * 将空白文本归一为 null。
     *
     * @param value 原始文本
     * @return 修剪后的文本；空白值返回 null
     */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 校验并修剪通知标识。
     *
     * @param notificationId 原始通知标识
     * @return 可传递给目录层的通知标识
     * @throws IllegalArgumentException 当通知标识缺失时抛出
     */
    private static String requireNotificationId(String notificationId) {
        if (notificationId == null || notificationId.isBlank()) {
            throw new IllegalArgumentException("notificationId is required");
        }
        return notificationId.trim();
    }
}
