package org.chovy.canvas.marketing.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 承载MarketingCampaignLinkDO对应的数据表字段。
 */
@TableName("marketing_campaign_link")
public class MarketingCampaignLinkDO {

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
     * 保存campaignId字段值。
     */
    private Long campaignId;

    /**
     * 保存resourceType字段值。
     */
    private String resourceType;

    /**
     * 保存resourceId字段值。
     */
    private Long resourceId;

    /**
     * 保存resourceKey字段值。
     */
    private String resourceKey;

    /**
     * 保存resourceName字段值。
     */
    private String resourceName;

    /**
     * 保存resourceRoute字段值。
     */
    private String resourceRoute;

    /**
     * 保存dependencyRole字段值。
     */
    private String dependencyRole;

    /**
     * 保存linkStatus字段值。
     */
    private String linkStatus;

    /**
     * 保存requiredForLaunch字段值。
     */
    private Integer requiredForLaunch;

    /**
     * 保存metadataJson字段值。
     */
    private String metadataJson;

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
     * 返回campaignId字段值。
     */
    public Long getCampaignId() {
        return campaignId;
    }

    /**
     * 设置campaignId字段值。
     */
    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    /**
     * 返回resourceType字段值。
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * 设置resourceType字段值。
     */
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    /**
     * 返回resourceId字段值。
     */
    public Long getResourceId() {
        return resourceId;
    }

    /**
     * 设置resourceId字段值。
     */
    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    /**
     * 返回resourceKey字段值。
     */
    public String getResourceKey() {
        return resourceKey;
    }

    /**
     * 设置resourceKey字段值。
     */
    public void setResourceKey(String resourceKey) {
        this.resourceKey = resourceKey;
    }

    /**
     * 返回resourceName字段值。
     */
    public String getResourceName() {
        return resourceName;
    }

    /**
     * 设置resourceName字段值。
     */
    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    /**
     * 返回resourceRoute字段值。
     */
    public String getResourceRoute() {
        return resourceRoute;
    }

    /**
     * 设置resourceRoute字段值。
     */
    public void setResourceRoute(String resourceRoute) {
        this.resourceRoute = resourceRoute;
    }

    /**
     * 返回dependencyRole字段值。
     */
    public String getDependencyRole() {
        return dependencyRole;
    }

    /**
     * 设置dependencyRole字段值。
     */
    public void setDependencyRole(String dependencyRole) {
        this.dependencyRole = dependencyRole;
    }

    /**
     * 返回linkStatus字段值。
     */
    public String getLinkStatus() {
        return linkStatus;
    }

    /**
     * 设置linkStatus字段值。
     */
    public void setLinkStatus(String linkStatus) {
        this.linkStatus = linkStatus;
    }

    /**
     * 返回requiredForLaunch字段值。
     */
    public Integer getRequiredForLaunch() {
        return requiredForLaunch;
    }

    /**
     * 设置requiredForLaunch字段值。
     */
    public void setRequiredForLaunch(Integer requiredForLaunch) {
        this.requiredForLaunch = requiredForLaunch;
    }

    /**
     * 返回metadataJson字段值。
     */
    public String getMetadataJson() {
        return metadataJson;
    }

    /**
     * 设置metadataJson字段值。
     */
    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
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
