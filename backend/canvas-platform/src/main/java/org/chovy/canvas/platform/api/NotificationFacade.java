package org.chovy.canvas.platform.api;

import java.util.List;
import java.util.Map;

/**
 * 提供站内通知查询、已读归档和 WebSocket 票据能力的应用入口。
 */
public interface NotificationFacade {

    /**
     * 分页查询操作者的通知。
     *
     * @param tenantId 租户标识
     * @param actor 操作者
     * @param unreadOnly 是否只查询未读通知
     * @param archived 是否查询已归档通知
     * @param category 通知分类
     * @param page 页码
     * @param size 每页数量
     * @return 通知分页结果
     */
    List<Map<String, Object>> list(Long tenantId, String actor, boolean unreadOnly, boolean archived, String category,
                                   int page, int size);

    /**
     * 查询操作者未读通知数量。
     *
     * @param tenantId 租户标识
     * @param actor 操作者
     * @return 未读数量记录
     */
    Map<String, Object> unreadCount(Long tenantId, String actor);

    /**
     * 将单条通知标记为已读。
     *
     * @param tenantId 租户标识
     * @param actor 操作者
     * @param notificationId 通知标识
     */
    void markRead(Long tenantId, String actor, String notificationId);

    /**
     * 将操作者全部通知标记为已读。
     *
     * @param tenantId 租户标识
     * @param actor 操作者
     */
    void markAllRead(Long tenantId, String actor);

    /**
     * 归档单条通知。
     *
     * @param tenantId 租户标识
     * @param actor 操作者
     * @param notificationId 通知标识
     */
    void archive(Long tenantId, String actor, String notificationId);

    /**
     * 创建用于订阅通知 WebSocket 的短期票据。
     *
     * @param tenantId 租户标识
     * @param actor 操作者
     * @return WebSocket 票据记录
     */
    Map<String, Object> createWsTicket(Long tenantId, String actor);
}
