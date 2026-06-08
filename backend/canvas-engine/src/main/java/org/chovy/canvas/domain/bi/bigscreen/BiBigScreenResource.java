package org.chovy.canvas.domain.bi.bigscreen;

import java.util.List;
import java.util.Map;

/**
 * BiBigScreenResource 承载 domain.bi.bigscreen 场景中的不可变数据快照。
 * @param id id 字段。
 * @param screenKey screenKey 字段。
 * @param name name 字段。
 * @param description description 字段。
 * @param size size 字段。
 * @param background background 字段。
 * @param layout layout 字段。
 * @param refresh refresh 字段。
 * @param mobileLayout mobileLayout 字段。
 * @param status status 字段。
 * @param version version 字段。
 * @param source source 字段。
 */
public record BiBigScreenResource(
        Long id,
        String screenKey,
        String name,
        String description,
        Map<String, Object> size,
        Map<String, Object> background,
        List<Map<String, Object>> layout,
        Map<String, Object> refresh,
        Map<String, Object> mobileLayout,
        String status,
        Integer version,
        String source
) {
    public BiBigScreenResource {
        size = size == null ? Map.of() : Map.copyOf(size);
        background = background == null ? Map.of() : Map.copyOf(background);
        layout = layout == null ? List.of() : List.copyOf(layout);
        refresh = refresh == null ? Map.of() : Map.copyOf(refresh);
        mobileLayout = mobileLayout == null ? Map.of() : Map.copyOf(mobileLayout);
    }
}
