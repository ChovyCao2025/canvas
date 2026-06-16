package org.chovy.canvas.marketing.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 表示MarketingCampaign的数据结构。
 */
public final class MarketingCampaign {

    /**
     * 记录的唯一标识。
     */
    private final Long id;

    /**
     * 所属租户标识。
     */
    private final Long tenantId;

    /**
     * 营销活动的稳定业务键。
     */
    private final CampaignKey campaignKey;

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
    private final CampaignStatus status;

    /**
     * 主要触达渠道。
     */
    private final String primaryChannel;

    /**
     * 负责该对象的团队。
     */
    private final String ownerTeam;

    /**
     * dateRange 字段值。
     */
    private final CampaignDateRange dateRange;

    /**
     * budget 字段值。
     */
    private final CampaignBudget budget;

    /**
     * 活动简报扩展信息。
     */
    private final Map<String, Object> brief;

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
     * 创建MarketingCampaign实例。
     */
    public MarketingCampaign(Long id, Long tenantId, CampaignKey campaignKey, String campaignName, String objective, CampaignStatus status, String primaryChannel, String ownerTeam, CampaignDateRange dateRange, CampaignBudget budget, Map<String, Object> brief, String createdBy, String updatedBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(campaignKey, "campaignKey");
        campaignName = defaultString(campaignName, campaignKey.value());
        objective = defaultString(objective, "UNSPECIFIED");
        status = status == null ? CampaignStatus.DRAFT : status;
        dateRange = dateRange == null ? CampaignDateRange.of(null, null) : dateRange;
        budget = budget == null ? CampaignBudget.of(null, null) : budget;
        brief = copyMap(brief);

        this.id = id;
        this.tenantId = tenantId;
        this.campaignKey = campaignKey;
        this.campaignName = campaignName;
        this.objective = objective;
        this.status = status;
        this.primaryChannel = primaryChannel;
        this.ownerTeam = ownerTeam;
        this.dateRange = dateRange;
        this.budget = budget;
        this.brief = brief;
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
     * 返回营销活动的稳定业务键。
     */
    public CampaignKey campaignKey() {
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
    public CampaignStatus status() {
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
     * 返回dateRange 字段值。
     */
    public CampaignDateRange dateRange() {
        return dateRange;
    }

    /**
     * 返回budget 字段值。
     */
    public CampaignBudget budget() {
        return budget;
    }

    /**
     * 返回活动简报扩展信息。
     */
    public Map<String, Object> brief() {
        return brief;
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
     * 创建existing业务对象。
     */
    public static MarketingCampaign createExisting(Long id,
                                                   Long tenantId,
                                                   CampaignKey campaignKey,
                                                   String campaignName,
                                                   String objective,
                                                   CampaignStatus status,
                                                   String primaryChannel,
                                                   String ownerTeam,
                                                   LocalDateTime startAt,
                                                   LocalDateTime endAt,
                                                   BigDecimal budgetAmount,
                                                   String currency,
                                                   Map<String, Object> brief,
                                                   String createdBy,
                                                   String updatedBy,
                                                   LocalDateTime createdAt,
                                                   LocalDateTime updatedAt) {
        return new MarketingCampaign(
                id,
                tenantId,
                campaignKey,
                campaignName,
                objective,
                status,
                primaryChannel,
                ownerTeam,
                CampaignDateRange.of(startAt, endAt),
                CampaignBudget.of(budgetAmount, currency),
                brief,
                createdBy,
                updatedBy,
                createdAt,
                updatedAt);
    }

    /**
     * 执行withId业务操作。
     */
    public MarketingCampaign withId(Long id) {
        return new MarketingCampaign(
                id,
                tenantId,
                campaignKey,
                campaignName,
                objective,
                status,
                primaryChannel,
                ownerTeam,
                dateRange,
                budget,
                brief,
                createdBy,
                updatedBy,
                createdAt,
                updatedAt);
    }

    /**
     * 执行defaultString业务操作。
     */
    private static String defaultString(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

    /**
     * 执行copyMap业务操作。
     */
    private static Map<String, Object> copyMap(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(value));
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
        MarketingCampaign that = (MarketingCampaign) o;
        return                 Objects.equals(id, that.id) &&
                Objects.equals(tenantId, that.tenantId) &&
                Objects.equals(campaignKey, that.campaignKey) &&
                Objects.equals(campaignName, that.campaignName) &&
                Objects.equals(objective, that.objective) &&
                Objects.equals(status, that.status) &&
                Objects.equals(primaryChannel, that.primaryChannel) &&
                Objects.equals(ownerTeam, that.ownerTeam) &&
                Objects.equals(dateRange, that.dateRange) &&
                Objects.equals(budget, that.budget) &&
                Objects.equals(brief, that.brief) &&
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
        return Objects.hash(id, tenantId, campaignKey, campaignName, objective, status, primaryChannel, ownerTeam, dateRange, budget, brief, createdBy, updatedBy, createdAt, updatedAt);
    }

    /**
     * 返回与记录类型一致的组件展示文本。
     */
    @Override
    public String toString() {
        return "MarketingCampaign[id=" + id + ", tenantId=" + tenantId + ", campaignKey=" + campaignKey + ", campaignName=" + campaignName + ", objective=" + objective + ", status=" + status + ", primaryChannel=" + primaryChannel + ", ownerTeam=" + ownerTeam + ", dateRange=" + dateRange + ", budget=" + budget + ", brief=" + brief + ", createdBy=" + createdBy + ", updatedBy=" + updatedBy + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
    }
}
