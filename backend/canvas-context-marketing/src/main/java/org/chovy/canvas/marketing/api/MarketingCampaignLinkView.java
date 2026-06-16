package org.chovy.canvas.marketing.api;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * 承载MarketingCampaignLinkView返回给调用方的只读视图。
 */
public final class MarketingCampaignLinkView {

    /**
     * 记录的唯一标识。
     */
    private final Long id;

    /**
     * 所属租户标识。
     */
    private final Long tenantId;

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
    private final boolean requiredForLaunch;

    /**
     * 扩展元数据。
     */
    private final Map<String, Object> metadata;

    /**
     * 创建人标识。
     */
    private final String createdBy;

    /**
     * 最后更新人标识。
     */
    private final String updatedBy;

    /**
     * 创建时间。
     */
    private final LocalDateTime createdAt;

    /**
     * 最后更新时间。
     */
    private final LocalDateTime updatedAt;

    /**
     * 创建MarketingCampaignLinkView实例。
     */
    public MarketingCampaignLinkView(Long id, Long tenantId, Long campaignId, String resourceType, Long resourceId, String resourceKey, String resourceName, String resourceRoute, String dependencyRole, String linkStatus, boolean requiredForLaunch, Map<String, Object> metadata, String createdBy, String updatedBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
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
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 返回记录的唯一标识。
     */
    public Long id() {
        return id;
    }

    /**
     * 返回所属租户标识。
     */
    public Long tenantId() {
        return tenantId;
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
    public boolean requiredForLaunch() {
        return requiredForLaunch;
    }

    /**
     * 返回扩展元数据。
     */
    public Map<String, Object> metadata() {
        return metadata;
    }

    /**
     * 返回创建人标识。
     */
    public String createdBy() {
        return createdBy;
    }

    /**
     * 返回最后更新人标识。
     */
    public String updatedBy() {
        return updatedBy;
    }

    /**
     * 返回创建时间。
     */
    public LocalDateTime createdAt() {
        return createdAt;
    }

    /**
     * 返回最后更新时间。
     */
    public LocalDateTime updatedAt() {
        return updatedAt;
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
        MarketingCampaignLinkView that = (MarketingCampaignLinkView) o;
        return                 Objects.equals(id, that.id) &&
                Objects.equals(tenantId, that.tenantId) &&
                Objects.equals(campaignId, that.campaignId) &&
                Objects.equals(resourceType, that.resourceType) &&
                Objects.equals(resourceId, that.resourceId) &&
                Objects.equals(resourceKey, that.resourceKey) &&
                Objects.equals(resourceName, that.resourceName) &&
                Objects.equals(resourceRoute, that.resourceRoute) &&
                Objects.equals(dependencyRole, that.dependencyRole) &&
                Objects.equals(linkStatus, that.linkStatus) &&
                requiredForLaunch == that.requiredForLaunch &&
                Objects.equals(metadata, that.metadata) &&
                Objects.equals(createdBy, that.createdBy) &&
                Objects.equals(updatedBy, that.updatedBy) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(updatedAt, that.updatedAt);
    }

    /**
     * 根据组件值计算哈希值。
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, tenantId, campaignId, resourceType, resourceId, resourceKey, resourceName, resourceRoute, dependencyRole, linkStatus, requiredForLaunch, metadata, createdBy, updatedBy, createdAt, updatedAt);
    }

    /**
     * 返回与记录类型一致的组件展示文本。
     */
    @Override
    public String toString() {
        return "MarketingCampaignLinkView[id=" + id + ", tenantId=" + tenantId + ", campaignId=" + campaignId + ", resourceType=" + resourceType + ", resourceId=" + resourceId + ", resourceKey=" + resourceKey + ", resourceName=" + resourceName + ", resourceRoute=" + resourceRoute + ", dependencyRole=" + dependencyRole + ", linkStatus=" + linkStatus + ", requiredForLaunch=" + requiredForLaunch + ", metadata=" + metadata + ", createdBy=" + createdBy + ", updatedBy=" + updatedBy + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
    }
}
