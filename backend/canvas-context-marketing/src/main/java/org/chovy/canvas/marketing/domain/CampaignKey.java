package org.chovy.canvas.marketing.domain;

import java.util.Locale;
import java.util.Objects;

/**
 * 表示CampaignKey使用的稳定匹配键。
 */
public final class CampaignKey implements Comparable<CampaignKey> {

    /**
     * value 字段值。
     */
    private final String value;

    /**
     * 创建CampaignKey实例。
     */
    public CampaignKey(String value) {
        value = normalize(value, "campaignKey");

        this.value = value;
    }

    /**
     * 返回value 字段值。
     */
    public String value() {
        return value;
    }




    /**
     * 执行of业务操作。
     */
    public static CampaignKey of(String value, String field) {
        return new CampaignKey(normalize(value, field));
    }

    /**
     * 转换为string对象。
     */
    @Override
    public String toString() {
        return value;
    }

    /**
     * 执行compareTo业务操作。
     */
    @Override
    public int compareTo(CampaignKey other) {
        return value.compareTo(other.value);
    }

    /**
     * 规范化输入值。
     */
    private static String normalize(String value, String field) {
        String normalized = required(value, field)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    /**
     * 校验并返回d必填值。
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 比较两个实例的组件值是否一致。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CampaignKey that = (CampaignKey) o;
        return                 Objects.equals(value, that.value);
    }

    /**
     * 根据组件值计算哈希值。
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
