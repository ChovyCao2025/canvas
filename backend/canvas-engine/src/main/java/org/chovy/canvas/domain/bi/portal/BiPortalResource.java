package org.chovy.canvas.domain.bi.portal;

import java.util.List;
import java.util.Map;

/**
 * BiPortalResource 承载 domain.bi.portal 场景中的不可变数据快照。
 * @param portalKey portalKey 字段。
 * @param name name 字段。
 * @param theme theme 字段。
 * @param menus menus 字段。
 * @param status status 字段。
 * @param source source 字段。
 */
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
