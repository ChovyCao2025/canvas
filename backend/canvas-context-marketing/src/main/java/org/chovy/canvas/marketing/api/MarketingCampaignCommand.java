package org.chovy.canvas.marketing.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * 承载MarketingCampaignCommand调用所需的输入参数。
 */
public final class MarketingCampaignCommand {

    /**
     * 营销活动的稳定业务键。
     */
    private final String campaignKey;

    /**
     * 营销活动展示名称。
     */
    private final String campaignName;

    /**
     * 营销活动目标。
     */
    private final String objective;

    /**
     * 当前业务状态。
     */
    private final String status;

    /**
     * 主要触达渠道。
     */
    private final String primaryChannel;

    /**
     * 负责该对象的团队。
     */
    private final String ownerTeam;

    /**
     * 开始时间。
     */
    private final LocalDateTime startAt;

    /**
     * 结束时间。
     */
    private final LocalDateTime endAt;

    /**
     * 预算金额。
     */
    private final BigDecimal budgetAmount;

    /**
     * 币种代码。
     */
    private final String currency;

    /**
     * 活动简报扩展信息。
     */
    private final Map<String, Object> brief;

    /**
     * 创建MarketingCampaignCommand实例。
     */
    public MarketingCampaignCommand(String campaignKey, String campaignName, String objective, String status, String primaryChannel, String ownerTeam, LocalDateTime startAt, LocalDateTime endAt, BigDecimal budgetAmount, String currency, Map<String, Object> brief) {
        this.campaignKey = campaignKey;
        this.campaignName = campaignName;
        this.objective = objective;
        this.status = status;
        this.primaryChannel = primaryChannel;
        this.ownerTeam = ownerTeam;
        this.startAt = startAt;
        this.endAt = endAt;
        this.budgetAmount = budgetAmount;
        this.currency = currency;
        this.brief = brief;
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
     * 返回营销活动目标。
     */
    public String objective() {
        return objective;
    }

    /**
     * 返回当前业务状态。
     */
    public String status() {
        return status;
    }

    /**
     * 返回主要触达渠道。
     */
    public String primaryChannel() {
        return primaryChannel;
    }

    /**
     * 返回负责该对象的团队。
     */
    public String ownerTeam() {
        return ownerTeam;
    }

    /**
     * 返回开始时间。
     */
    public LocalDateTime startAt() {
        return startAt;
    }

    /**
     * 返回结束时间。
     */
    public LocalDateTime endAt() {
        return endAt;
    }

    /**
     * 返回预算金额。
     */
    public BigDecimal budgetAmount() {
        return budgetAmount;
    }

    /**
     * 返回币种代码。
     */
    public String currency() {
        return currency;
    }

    /**
     * 返回活动简报扩展信息。
     */
    public Map<String, Object> brief() {
        return brief;
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
        MarketingCampaignCommand that = (MarketingCampaignCommand) o;
        return                 Objects.equals(campaignKey, that.campaignKey) &&
                Objects.equals(campaignName, that.campaignName) &&
                Objects.equals(objective, that.objective) &&
                Objects.equals(status, that.status) &&
                Objects.equals(primaryChannel, that.primaryChannel) &&
                Objects.equals(ownerTeam, that.ownerTeam) &&
                Objects.equals(startAt, that.startAt) &&
                Objects.equals(endAt, that.endAt) &&
                Objects.equals(budgetAmount, that.budgetAmount) &&
                Objects.equals(currency, that.currency) &&
                Objects.equals(brief, that.brief);
    }

    /**
     * 根据组件值计算哈希值。
     */
    @Override
    public int hashCode() {
        return Objects.hash(campaignKey, campaignName, objective, status, primaryChannel, ownerTeam, startAt, endAt, budgetAmount, currency, brief);
    }

    /**
     * 返回与记录类型一致的组件展示文本。
     */
    @Override
    public String toString() {
        return "MarketingCampaignCommand[campaignKey=" + campaignKey + ", campaignName=" + campaignName + ", objective=" + objective + ", status=" + status + ", primaryChannel=" + primaryChannel + ", ownerTeam=" + ownerTeam + ", startAt=" + startAt + ", endAt=" + endAt + ", budgetAmount=" + budgetAmount + ", currency=" + currency + ", brief=" + brief + "]";
    }
}
