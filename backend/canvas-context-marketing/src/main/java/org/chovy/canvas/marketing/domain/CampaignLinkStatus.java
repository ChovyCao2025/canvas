package org.chovy.canvas.marketing.domain;

import java.util.Locale;

/**
 * 枚举CampaignLinkStatus支持的业务状态。
 */
public enum CampaignLinkStatus {
    /**
     * 已启用状态。
     */
    ACTIVE,
    /**
     * 资源缺失状态。
     */
    MISSING,
    /**
     * 资源阻断状态。
     */
    BLOCKED,
    /**
     * 已归档状态。
     */
    ARCHIVED;

    /**
     * 根据输入创建对象。
     */
    public static CampaignLinkStatus from(String value) {
        String status = normalizeUpper(value, "ACTIVE");
        return switch (status) {
            case "ACTIVE" -> ACTIVE;
            case "MISSING" -> MISSING;
            case "BLOCKED" -> BLOCKED;
            case "ARCHIVED" -> ARCHIVED;
            default -> throw new IllegalArgumentException("unsupported link status: " + status);
        };
    }

    /**
     * 规范化upper输入值。
     */
    private static String normalizeUpper(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed.toUpperCase(Locale.ROOT);
    }
}
