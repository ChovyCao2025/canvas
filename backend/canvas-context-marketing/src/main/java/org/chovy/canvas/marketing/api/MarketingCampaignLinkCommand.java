package org.chovy.canvas.marketing.api;

import java.util.Map;
import java.util.Objects;

/**
 * 承载MarketingCampaignLinkCommand调用所需的输入参数。
 */
public final class MarketingCampaignLinkCommand {

    /**
     * 关联的营销活动标识。
     */
    private final Long campaignId;

    /**
     * 关联资源类型。
     */
    private final String resourceType;

    /**
     * 关联资源标识。
     */
    private final Long resourceId;

    /**
     * 关联资源业务键。
     */
    private final String resourceKey;

    /**
     * 关联资源展示名称。
     */
    private final String resourceName;

    /**
     * 关联资源前端路由。
     */
    private final String resourceRoute;

    /**
     * 资源在投放准备中的依赖角色。
     */
    private final String dependencyRole;

    /**
     * 资源关联状态。
     */
    private final String linkStatus;

    /**
     * 是否为上线必需资源。
     */
    private final Boolean requiredForLaunch;

    /**
     * 扩展元数据。
     */
    private final Map<String, Object> metadata;

    /**
     * 创建MarketingCampaignLinkCommand实例。
     */
    public MarketingCampaignLinkCommand(Long campaignId, String resourceType, Long resourceId, String resourceKey, String resourceName, String resourceRoute, String dependencyRole, String linkStatus, Boolean requiredForLaunch, Map<String, Object> metadata) {
        this.campaignId = campaignId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.resourceKey = resourceKey;
        this.resourceName = resourceName;
        this.resourceRoute = resourceRoute;
        this.dependencyRole = dependencyRole;
        this.linkStatus = linkStatus;
        this.requiredForLaunch = requiredForLaunch;
        this.metadata = metadata;
    }

    /**
     * 返回关联的营销活动标识。
     */
    public Long campaignId() {
        return campaignId;
    }

    /**
     * 返回关联资源类型。
     */
    public String resourceType() {
        return resourceType;
    }

    /**
     * 返回关联资源标识。
     */
    public Long resourceId() {
        return resourceId;
    }

    /**
     * 返回关联资源业务键。
     */
    public String resourceKey() {
        return resourceKey;
    }

    /**
     * 返回关联资源展示名称。
     */
    public String resourceName() {
        return resourceName;
    }

    /**
     * 返回关联资源前端路由。
     */
    public String resourceRoute() {
        return resourceRoute;
    }

    /**
     * 返回资源在投放准备中的依赖角色。
     */
    public String dependencyRole() {
        return dependencyRole;
    }

    /**
     * 返回资源关联状态。
     */
    public String linkStatus() {
        return linkStatus;
    }

    /**
     * 返回是否为上线必需资源。
     */
    public Boolean requiredForLaunch() {
        return requiredForLaunch;
    }

    /**
     * 返回扩展元数据。
     */
    public Map<String, Object> metadata() {
        return metadata;
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
        MarketingCampaignLinkCommand that = (MarketingCampaignLinkCommand) o;
        return                 Objects.equals(campaignId, that.campaignId) &&
                Objects.equals(resourceType, that.resourceType) &&
                Objects.equals(resourceId, that.resourceId) &&
                Objects.equals(resourceKey, that.resourceKey) &&
                Objects.equals(resourceName, that.resourceName) &&
                Objects.equals(resourceRoute, that.resourceRoute) &&
                Objects.equals(dependencyRole, that.dependencyRole) &&
                Objects.equals(linkStatus, that.linkStatus) &&
                Objects.equals(requiredForLaunch, that.requiredForLaunch) &&
                Objects.equals(metadata, that.metadata);
    }

    /**
     * 根据组件值计算哈希值。
     */
    @Override
    public int hashCode() {
        return Objects.hash(campaignId, resourceType, resourceId, resourceKey, resourceName, resourceRoute, dependencyRole, linkStatus, requiredForLaunch, metadata);
    }

    /**
     * 返回与记录类型一致的组件展示文本。
     */
    @Override
    public String toString() {
        return "MarketingCampaignLinkCommand[campaignId=" + campaignId + ", resourceType=" + resourceType + ", resourceId=" + resourceId + ", resourceKey=" + resourceKey + ", resourceName=" + resourceName + ", resourceRoute=" + resourceRoute + ", dependencyRole=" + dependencyRole + ", linkStatus=" + linkStatus + ", requiredForLaunch=" + requiredForLaunch + ", metadata=" + metadata + "]";
    }
}
