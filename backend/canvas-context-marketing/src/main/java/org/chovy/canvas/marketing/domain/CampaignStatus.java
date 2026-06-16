package org.chovy.canvas.marketing.domain;

import java.util.Locale;

/**
 * 枚举CampaignStatus支持的业务状态。
 */
public enum CampaignStatus {
    /**
     * 草稿状态。
     */
    DRAFT,
    /**
     * 已启用状态。
     */
    ACTIVE,
    /**
     * 暂停状态。
     */
    PAUSED,
    /**
     * 已完成状态。
     */
    COMPLETED,
    /**
     * 已归档状态。
     */
    ARCHIVED;

    /**
     * 根据输入创建对象。
     */
    public static CampaignStatus from(String value) {
        String status = normalizeUpper(value, "DRAFT");
        return switch (status) {
            case "DRAFT" -> DRAFT;
            case "ACTIVE" -> ACTIVE;
            case "PAUSED" -> PAUSED;
            case "COMPLETED" -> COMPLETED;
            case "ARCHIVED" -> ARCHIVED;
            default -> throw new IllegalArgumentException("unsupported campaign status: " + status);
        };
    }

    /**
     * 执行optional业务操作。
     */
    public static CampaignStatus optional(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : from(trimmed);
    }

    /**
     * 规范化upper输入值。
     */
    private static String normalizeUpper(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed.toUpperCase(Locale.ROOT);
    }
}
