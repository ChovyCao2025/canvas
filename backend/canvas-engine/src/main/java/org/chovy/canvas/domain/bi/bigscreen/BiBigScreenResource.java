package org.chovy.canvas.domain.bi.bigscreen;

import java.util.List;
import java.util.Map;

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
