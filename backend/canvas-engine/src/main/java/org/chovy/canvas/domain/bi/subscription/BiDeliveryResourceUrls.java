package org.chovy.canvas.domain.bi.subscription;

import java.util.Locale;

final class BiDeliveryResourceUrls {

    private BiDeliveryResourceUrls() {
    }

    static String workbenchUrl(String resourceType, Long resourceId) {
        String type = resourceType == null ? "" : resourceType.trim().toUpperCase(Locale.ROOT);
        String url = "/bi?resourceType=" + type + "&resourceId=" + resourceId;
        return switch (type) {
            case "BIG_SCREEN" -> url + "&mode=big-screen";
            case "SPREADSHEET" -> url + "&mode=spreadsheet";
            default -> url;
        };
    }
}
