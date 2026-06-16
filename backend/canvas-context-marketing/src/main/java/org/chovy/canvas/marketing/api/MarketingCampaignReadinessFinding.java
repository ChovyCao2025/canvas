package org.chovy.canvas.marketing.api;
import java.util.Objects;

/**
 * 描述MarketingCampaignReadinessFinding中的单个发现。
 */
public final class MarketingCampaignReadinessFinding {

    /**
     * 问题严重级别。
     */
    private final String severity;

    /**
     * 问题所属对象类型。
     */
    private final String itemType;

    /**
     * 问题所属对象键。
     */
    private final String itemKey;

    /**
     * 问题标题。
     */
    private final String title;

    /**
     * 问题原因。
     */
    private final String reason;

    /**
     * 可跳转处理路由。
     */
    private final String route;

    /**
     * 创建MarketingCampaignReadinessFinding实例。
     */
    public MarketingCampaignReadinessFinding(String severity, String itemType, String itemKey, String title, String reason, String route) {
        this.severity = severity;
        this.itemType = itemType;
        this.itemKey = itemKey;
        this.title = title;
        this.reason = reason;
        this.route = route;
    }

    /**
     * 返回问题严重级别。
     */
    public String severity() {
        return severity;
    }

    /**
     * 返回问题所属对象类型。
     */
    public String itemType() {
        return itemType;
    }

    /**
     * 返回问题所属对象键。
     */
    public String itemKey() {
        return itemKey;
    }

    /**
     * 返回问题标题。
     */
    public String title() {
        return title;
    }

    /**
     * 返回问题原因。
     */
    public String reason() {
        return reason;
    }

    /**
     * 返回可跳转处理路由。
     */
    public String route() {
        return route;
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
        MarketingCampaignReadinessFinding that = (MarketingCampaignReadinessFinding) o;
        return                 Objects.equals(severity, that.severity) &&
                Objects.equals(itemType, that.itemType) &&
                Objects.equals(itemKey, that.itemKey) &&
                Objects.equals(title, that.title) &&
                Objects.equals(reason, that.reason) &&
                Objects.equals(route, that.route);
    }

    /**
     * 根据组件值计算哈希值。
     */
    @Override
    public int hashCode() {
        return Objects.hash(severity, itemType, itemKey, title, reason, route);
    }

    /**
     * 返回与记录类型一致的组件展示文本。
     */
    @Override
    public String toString() {
        return "MarketingCampaignReadinessFinding[severity=" + severity + ", itemType=" + itemType + ", itemKey=" + itemKey + ", title=" + title + ", reason=" + reason + ", route=" + route + "]";
    }
}
