package org.chovy.canvas.domain.bi.portal;

import java.util.Map;

/**
 * BiPortalMenuResource 承载 domain.bi.portal 场景中的不可变数据快照。
 * @param menuKey menuKey 字段。
 * @param parentMenuKey parentMenuKey 字段。
 * @param title title 字段。
 * @param resourceType resourceType 字段。
 * @param resourceKey resourceKey 字段。
 * @param resourceId resourceId 字段。
 * @param externalUrl externalUrl 字段。
 * @param visibility visibility 字段。
 * @param sortOrder sortOrder 字段。
 */
public record BiPortalMenuResource(
        String menuKey,
        String parentMenuKey,
        String title,
        String resourceType,
        String resourceKey,
        Long resourceId,
        String externalUrl,
        Map<String, Object> visibility,
        int sortOrder
) {
    public BiPortalMenuResource {
        visibility = visibility == null ? Map.of() : Map.copyOf(visibility);
    }
}
