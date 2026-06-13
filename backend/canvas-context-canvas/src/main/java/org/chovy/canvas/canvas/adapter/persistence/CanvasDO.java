package org.chovy.canvas.canvas.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("canvas")
public class CanvasDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("tenant_id")
    private Long tenantId;
    private String name;
    private String description;
    private Integer status;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long publishedVersionId;
    private String createdBy;
    private Integer isExample;
    private String sourceTemplateKey;
    private String projectKey;
    private String projectName;
    private String folderKey;
    private String folderName;
    private LocalDateTime validStart;
    private LocalDateTime validEnd;
    private Integer perUserTotalLimit;
    private Integer perUserDailyLimit;
    private Integer cooldownSeconds;
    private Integer controlGroupPercent;
    private String controlGroupSalt;
    private String conversionEventCode;
    private Integer attributionWindowDays;
    private String attributionModel;
    private Integer maxTotalExecutions;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long canaryVersionId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer canaryPercent;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long previousVersionId;
    private Integer editVersion;
    private String triggerType;
    private String cronExpression;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getPublishedVersionId() {
        return publishedVersionId;
    }

    public void setPublishedVersionId(Long publishedVersionId) {
        this.publishedVersionId = publishedVersionId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getValidStart() {
        return validStart;
    }

    public void setValidStart(LocalDateTime validStart) {
        this.validStart = validStart;
    }

    public LocalDateTime getValidEnd() {
        return validEnd;
    }

    public void setValidEnd(LocalDateTime validEnd) {
        this.validEnd = validEnd;
    }

    public Integer getPerUserTotalLimit() {
        return perUserTotalLimit;
    }

    public void setPerUserTotalLimit(Integer perUserTotalLimit) {
        this.perUserTotalLimit = perUserTotalLimit;
    }

    public Integer getPerUserDailyLimit() {
        return perUserDailyLimit;
    }

    public void setPerUserDailyLimit(Integer perUserDailyLimit) {
        this.perUserDailyLimit = perUserDailyLimit;
    }

    public Integer getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(Integer cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    public Integer getControlGroupPercent() {
        return controlGroupPercent;
    }

    public void setControlGroupPercent(Integer controlGroupPercent) {
        this.controlGroupPercent = controlGroupPercent;
    }

    public String getControlGroupSalt() {
        return controlGroupSalt;
    }

    public void setControlGroupSalt(String controlGroupSalt) {
        this.controlGroupSalt = controlGroupSalt;
    }

    public String getConversionEventCode() {
        return conversionEventCode;
    }

    public void setConversionEventCode(String conversionEventCode) {
        this.conversionEventCode = conversionEventCode;
    }

    public Integer getAttributionWindowDays() {
        return attributionWindowDays;
    }

    public void setAttributionWindowDays(Integer attributionWindowDays) {
        this.attributionWindowDays = attributionWindowDays;
    }

    public String getAttributionModel() {
        return attributionModel;
    }

    public void setAttributionModel(String attributionModel) {
        this.attributionModel = attributionModel;
    }

    public Integer getMaxTotalExecutions() {
        return maxTotalExecutions;
    }

    public void setMaxTotalExecutions(Integer maxTotalExecutions) {
        this.maxTotalExecutions = maxTotalExecutions;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public Long getCanaryVersionId() {
        return canaryVersionId;
    }

    public void setCanaryVersionId(Long canaryVersionId) {
        this.canaryVersionId = canaryVersionId;
    }

    public Integer getCanaryPercent() {
        return canaryPercent;
    }

    public void setCanaryPercent(Integer canaryPercent) {
        this.canaryPercent = canaryPercent;
    }

    public Long getPreviousVersionId() {
        return previousVersionId;
    }

    public void setPreviousVersionId(Long previousVersionId) {
        this.previousVersionId = previousVersionId;
    }
}
