package org.chovy.canvas.domain.bi.resource;

/**
 * BiResourceFavoriteCommand 承载 domain.bi.resource 场景中的不可变数据快照。
 * @param resourceType resourceType 字段。
 * @param resourceKey resourceKey 字段。
 * @param favorite favorite 字段。
 */
public record BiResourceFavoriteCommand(
        String resourceType,
        String resourceKey,
        Boolean favorite) {

    /**
     * 创建 BiResourceFavoriteCommand 实例并注入 domain.bi.resource 场景依赖。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceKey 业务键，用于在同一租户下定位资源。
     */
    public BiResourceFavoriteCommand(String resourceType, String resourceKey) {
        this(resourceType, resourceKey, true);
    }
}
