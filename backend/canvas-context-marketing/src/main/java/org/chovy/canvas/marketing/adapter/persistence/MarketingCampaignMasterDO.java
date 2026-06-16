package org.chovy.canvas.marketing.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 承载MarketingCampaignMasterDO对应的数据表字段。
 */
@TableName("marketing_campaign_master")
public class MarketingCampaignMasterDO {

    /**
     * 保存id字段值。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 保存tenantId字段值。
     */
    private Long tenantId;

    /**
     * 保存campaignKey字段值。
     */
    private String campaignKey;

    /**
     * 保存campaignName字段值。
     */
    private String campaignName;

    /**
     * 保存objective字段值。
     */
    private String objective;

    /**
     * 保存status字段值。
     */
    private String status;

    /**
     * 保存primaryChannel字段值。
     */
    private String primaryChannel;

    /**
     * 保存ownerTeam字段值。
     */
    private String ownerTeam;

    /**
     * 保存startAt字段值。
     */
    private LocalDateTime startAt;

    /**
     * 保存endAt字段值。
     */
    private LocalDateTime endAt;

    /**
     * 保存budgetAmount字段值。
     */
    private BigDecimal budgetAmount;

    /**
     * 保存currency字段值。
     */
    private String currency;

    /**
     * 保存briefJson字段值。
     */
    private String briefJson;

    /**
     * 保存createdBy字段值。
     */
    private String createdBy;

    /**
     * 保存updatedBy字段值。
     */
    private String updatedBy;

    /**
     * 保存createdAt字段值。
     */
    private LocalDateTime createdAt;

    /**
     * 保存updatedAt字段值。
     */
    private LocalDateTime updatedAt;

    /**
     * 返回id字段值。
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置id字段值。
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 返回tenantId字段值。
     */
    public Long getTenantId() {
        return tenantId;
    }

    /**
     * 设置tenantId字段值。
     */
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * 返回campaignKey字段值。
     */
    public String getCampaignKey() {
        return campaignKey;
    }

    /**
     * 设置campaignKey字段值。
     */
    public void setCampaignKey(String campaignKey) {
        this.campaignKey = campaignKey;
    }

    /**
     * 返回campaignName字段值。
     */
    public String getCampaignName() {
        return campaignName;
    }

    /**
     * 设置campaignName字段值。
     */
    public void setCampaignName(String campaignName) {
        this.campaignName = campaignName;
    }

    /**
     * 返回objective字段值。
     */
    public String getObjective() {
        return objective;
    }

    /**
     * 设置objective字段值。
     */
    public void setObjective(String objective) {
        this.objective = objective;
    }

    /**
     * 返回status字段值。
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置status字段值。
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 返回primaryChannel字段值。
     */
    public String getPrimaryChannel() {
        return primaryChannel;
    }

    /**
     * 设置primaryChannel字段值。
     */
    public void setPrimaryChannel(String primaryChannel) {
        this.primaryChannel = primaryChannel;
    }

    /**
     * 返回ownerTeam字段值。
     */
    public String getOwnerTeam() {
        return ownerTeam;
    }

    /**
     * 设置ownerTeam字段值。
     */
    public void setOwnerTeam(String ownerTeam) {
        this.ownerTeam = ownerTeam;
    }

    /**
     * 返回startAt字段值。
     */
    public LocalDateTime getStartAt() {
        return startAt;
    }

    /**
     * 设置startAt字段值。
     */
    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    /**
     * 返回endAt字段值。
     */
    public LocalDateTime getEndAt() {
        return endAt;
    }

    /**
     * 设置endAt字段值。
     */
    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    /**
     * 返回budgetAmount字段值。
     */
    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    /**
     * 设置budgetAmount字段值。
     */
    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    /**
     * 返回currency字段值。
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * 设置currency字段值。
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * 返回briefJson字段值。
     */
    public String getBriefJson() {
        return briefJson;
    }

    /**
     * 设置briefJson字段值。
     */
    public void setBriefJson(String briefJson) {
        this.briefJson = briefJson;
    }

    /**
     * 返回createdBy字段值。
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * 设置createdBy字段值。
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * 返回updatedBy字段值。
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * 设置updatedBy字段值。
     */
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    /**
     * 返回createdAt字段值。
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置createdAt字段值。
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 返回updatedAt字段值。
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置updatedAt字段值。
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
