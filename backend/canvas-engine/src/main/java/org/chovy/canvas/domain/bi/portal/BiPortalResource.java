package org.chovy.canvas.domain.bi.portal;

import java.util.List;
import java.util.Map;

public record BiPortalResource(
        String portalKey,
        String name,
        Map<String, Object> theme,
        List<BiPortalMenuResource> menus,
        String status,
        String source
) {
    public BiPortalResource {
        theme = theme == null ? Map.of() : Map.copyOf(theme);
        menus = menus == null ? List.of() : List.copyOf(menus);
    }
}
