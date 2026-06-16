package org.chovy.canvas.marketing.api;

import java.util.List;
import java.util.Objects;

/**
 * 承载MarketingCampaignReadinessView返回给调用方的只读视图。
 */
public final class MarketingCampaignReadinessView {

    /**
     * 所属租户标识。
     */
    private final Long tenantId;

    /**
     * 关联的营销活动标识。
     */
    private final Long campaignId;

    /**
     * 营销活动的稳定业务键。
     */
    private final String campaignKey;

    /**
     * 营销活动展示名称。
     */
    private final String campaignName;

    /**
     * 报告生成时间。
     */
    private final String generatedAt;

    /**
     * 当前业务状态。
     */
    private final String status;

    /**
     * 是否满足生产上线条件。
     */
    private final boolean productionReady;

    /**
     * 必需资源关联总数。
     */
    private final int requiredLinkCount;

    /**
     * 已激活的必需资源关联数。
     */
    private final int activeRequiredLinkCount;

    /**
     * 阻断项数量。
     */
    private final int blockerCount;

    /**
     * 警告项数量。
     */
    private final int warningCount;

    /**
     * 阻断问题列表。
     */
    private final List<MarketingCampaignReadinessFinding> blockers;

    /**
     * 警告问题列表。
     */
    private final List<MarketingCampaignReadinessFinding> warnings;

    /**
     * 资源关联明细。
     */
    private final List<MarketingCampaignLinkView> links;

    /**
     * 创建MarketingCampaignReadinessView实例。
     */
    public MarketingCampaignReadinessView(Long tenantId, Long campaignId, String campaignKey, String campaignName, String generatedAt, String status, boolean productionReady, int requiredLinkCount, int activeRequiredLinkCount, int blockerCount, int warningCount, List<MarketingCampaignReadinessFinding> blockers, List<MarketingCampaignReadinessFinding> warnings, List<MarketingCampaignLinkView> links) {
        this.tenantId = tenantId;
        this.campaignId = campaignId;
        this.campaignKey = campaignKey;
        this.campaignName = campaignName;
        this.generatedAt = generatedAt;
        this.status = status;
        this.productionReady = productionReady;
        this.requiredLinkCount = requiredLinkCount;
        this.activeRequiredLinkCount = activeRequiredLinkCount;
        this.blockerCount = blockerCount;
        this.warningCount = warningCount;
        this.blockers = blockers;
        this.warnings = warnings;
        this.links = links;
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
     * 返回营销活动的稳定业务键。
     */
    public String campaignKey() {
        return campaignKey;
    }

    /**
     * 返回营销活动展示名称。
     */
    public String campaignName() {
        return campaignName;
    }

    /**
     * 返回报告生成时间。
     */
    public String generatedAt() {
        return generatedAt;
    }

    /**
     * 返回当前业务状态。
     */
    public String status() {
        return status;
    }

    /**
     * 返回是否满足生产上线条件。
     */
    public boolean productionReady() {
        return productionReady;
    }

    /**
     * 返回必需资源关联总数。
     */
    public int requiredLinkCount() {
        return requiredLinkCount;
    }

    /**
     * 返回已激活的必需资源关联数。
     */
    public int activeRequiredLinkCount() {
        return activeRequiredLinkCount;
    }

    /**
     * 返回阻断项数量。
     */
    public int blockerCount() {
        return blockerCount;
    }

    /**
     * 返回警告项数量。
     */
    public int warningCount() {
        return warningCount;
    }

    /**
     * 返回阻断问题列表。
     */
    public List<MarketingCampaignReadinessFinding> blockers() {
        return blockers;
    }

    /**
     * 返回警告问题列表。
     */
    public List<MarketingCampaignReadinessFinding> warnings() {
        return warnings;
    }

    /**
     * 返回资源关联明细。
     */
    public List<MarketingCampaignLinkView> links() {
        return links;
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
        MarketingCampaignReadinessView that = (MarketingCampaignReadinessView) o;
        return                 Objects.equals(tenantId, that.tenantId) &&
                Objects.equals(campaignId, that.campaignId) &&
                Objects.equals(campaignKey, that.campaignKey) &&
                Objects.equals(campaignName, that.campaignName) &&
                Objects.equals(generatedAt, that.generatedAt) &&
                Objects.equals(status, that.status) &&
                productionReady == that.productionReady &&
                requiredLinkCount == that.requiredLinkCount &&
                activeRequiredLinkCount == that.activeRequiredLinkCount &&
                blockerCount == that.blockerCount &&
                warningCount == that.warningCount &&
                Objects.equals(blockers, that.blockers) &&
                Objects.equals(warnings, that.warnings) &&
                Objects.equals(links, that.links);
    }

    /**
     * 根据组件值计算哈希值。
     */
    @Override
    public int hashCode() {
        return Objects.hash(tenantId, campaignId, campaignKey, campaignName, generatedAt, status, productionReady, requiredLinkCount, activeRequiredLinkCount, blockerCount, warningCount, blockers, warnings, links);
    }

    /**
     * 返回与记录类型一致的组件展示文本。
     */
    @Override
    public String toString() {
        return "MarketingCampaignReadinessView[tenantId=" + tenantId + ", campaignId=" + campaignId + ", campaignKey=" + campaignKey + ", campaignName=" + campaignName + ", generatedAt=" + generatedAt + ", status=" + status + ", productionReady=" + productionReady + ", requiredLinkCount=" + requiredLinkCount + ", activeRequiredLinkCount=" + activeRequiredLinkCount + ", blockerCount=" + blockerCount + ", warningCount=" + warningCount + ", blockers=" + blockers + ", warnings=" + warnings + ", links=" + links + "]";
    }
}
