package org.chovy.canvas.domain.bi.resource;

import java.time.LocalDateTime;

/**
 * BiResourceFavoriteView 承载 domain.bi.resource 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param workspaceId workspaceId 字段。
 * @param resourceType resourceType 字段。
 * @param resourceKey resourceKey 字段。
 * @param username username 字段。
 * @param favorite favorite 字段。
 * @param createdAt createdAt 字段。
 */
public record BiResourceFavoriteView(
        Long id,
        Long tenantId,
        Long workspaceId,
        String resourceType,
        String resourceKey,
        String username,
        Boolean favorite,
        LocalDateTime createdAt) {

    /**
     * 创建 BiResourceFavoriteView 实例并注入 domain.bi.resource 场景依赖。
     * @param id 业务对象 ID，用于定位具体记录。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceKey 业务键，用于在同一租户下定位资源。
     * @param username 操作人标识，用于审计和权限判断。
     * @param createdAt 时间参数，用于计算窗口、过期或审计时间。
     */
    public BiResourceFavoriteView(Long id,
                                  Long tenantId,
                                  Long workspaceId,
                                  String resourceType,
                                  String resourceKey,
                                  String username,
                                  LocalDateTime createdAt) {
        this(id, tenantId, workspaceId, resourceType, resourceKey, username, true, createdAt);
    }
}
