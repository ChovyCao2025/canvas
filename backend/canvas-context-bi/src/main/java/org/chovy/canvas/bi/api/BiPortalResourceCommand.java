package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;

public record BiPortalResourceCommand(
        String portalKey,
        String title,
        String description,
        List<String> dashboardKeys,
        Map<String, Object> layout,
        Map<String, Object> settings,
        String status) {
}
