package org.chovy.canvas.domain.bi.portal;

import java.util.Map;

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
