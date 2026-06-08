package org.chovy.canvas.domain.bi.subscription;

import java.util.Locale;

/**
 * BiDeliveryResourceUrls 承载对应领域的业务规则、流程编排和结果转换。
 */
final class BiDeliveryResourceUrls {

    /**
     * 初始化 BiDeliveryResourceUrls 实例。
     */
    private BiDeliveryResourceUrls() {
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceId 业务对象 ID，用于定位具体记录。
     * @return 返回 workbench url 生成的文本或业务键。
     */
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
