package org.chovy.canvas.canvas.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 封装CanvasDO相关的业务逻辑。
 */
@TableName("canvas")
public class CanvasDO {

    /**
     * 保存标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 保存租户标识。
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 保存名称。
     */
    private String name;

    /**
     * 保存描述。
     */
    private String description;

    /**
     * 保存状态。
     */
    private Integer status;

    /**
     * 保存published version标识。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long publishedVersionId;

    /**
     * 保存创建人。
     */
    private String createdBy;

    /**
     * 保存isExample。
     */
    private Integer isExample;

    /**
     * 保存sourceTemplateKey。
     */
    private String sourceTemplateKey;

    /**
     * 保存projectKey。
     */
    private String projectKey;

    /**
     * 保存projectName。
     */
    private String projectName;

    /**
     * 保存folderKey。
     */
    private String folderKey;

    /**
     * 保存folderName。
     */
    private String folderName;

    /**
     * 保存validStart。
     */
    private LocalDateTime validStart;

    /**
     * 保存validEnd。
     */
    private LocalDateTime validEnd;

    /**
     * 保存perUserTotalLimit。
     */
    private Integer perUserTotalLimit;

    /**
     * 保存perUserDailyLimit。
     */
    private Integer perUserDailyLimit;

    /**
     * 保存cooldownSeconds。
     */
    private Integer cooldownSeconds;

    /**
     * 保存controlGroupPercent。
     */
    private Integer controlGroupPercent;

    /**
     * 保存controlGroupSalt。
     */
    private String controlGroupSalt;

    /**
     * 保存conversionEventCode。
     */
    private String conversionEventCode;

    /**
     * 保存attributionWindowDays。
     */
    private Integer attributionWindowDays;

    /**
     * 保存attributionModel。
     */
    private String attributionModel;

    /**
     * 保存maxTotalExecutions。
     */
    private Integer maxTotalExecutions;

    /**
     * 保存canary version标识。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long canaryVersionId;

    /**
     * 保存canaryPercent。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer canaryPercent;

    /**
     * 保存previous version标识。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long previousVersionId;

    /**
     * 保存editVersion。
     */
    private Integer editVersion;

    /**
     * 保存triggerType。
     */
    private String triggerType;

    /**
     * 保存cronExpression。
     */
    private String cronExpression;

    /**
     * 保存创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 保存更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 获取标识。
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置标识。
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取租户标识。
     */
    public Long getTenantId() {
        return tenantId;
    }

    /**
     * 设置租户标识。
     */
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * 获取名称。
     */
    public String getName() {
        return name;
    }

    /**
     * 设置名称。
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取描述。
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置描述。
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取状态。
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 设置状态。
     */
    public void setStatus(Integer status) {
        this.status = status;
    }

    /**
     * 获取published version标识。
     */
    public Long getPublishedVersionId() {
        return publishedVersionId;
    }

    /**
     * 设置published version标识。
     */
    public void setPublishedVersionId(Long publishedVersionId) {
        this.publishedVersionId = publishedVersionId;
    }

    /**
     * 获取创建人。
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * 设置创建人。
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * 获取ValidStart。
     */
    public LocalDateTime getValidStart() {
        return validStart;
    }

    /**
     * 设置ValidStart。
     */
    public void setValidStart(LocalDateTime validStart) {
        this.validStart = validStart;
    }

    /**
     * 获取ValidEnd。
     */
    public LocalDateTime getValidEnd() {
        return validEnd;
    }

    /**
     * 设置ValidEnd。
     */
    public void setValidEnd(LocalDateTime validEnd) {
        this.validEnd = validEnd;
    }

    /**
     * 获取PerUserTotalLimit。
     */
    public Integer getPerUserTotalLimit() {
        return perUserTotalLimit;
    }

    /**
     * 设置PerUserTotalLimit。
     */
    public void setPerUserTotalLimit(Integer perUserTotalLimit) {
        this.perUserTotalLimit = perUserTotalLimit;
    }

    /**
     * 获取PerUserDailyLimit。
     */
    public Integer getPerUserDailyLimit() {
        return perUserDailyLimit;
    }

    /**
     * 设置PerUserDailyLimit。
     */
    public void setPerUserDailyLimit(Integer perUserDailyLimit) {
        this.perUserDailyLimit = perUserDailyLimit;
    }

    /**
     * 获取CooldownSeconds。
     */
    public Integer getCooldownSeconds() {
        return cooldownSeconds;
    }

    /**
     * 设置CooldownSeconds。
     */
    public void setCooldownSeconds(Integer cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    /**
     * 获取ControlGroupPercent。
     */
    public Integer getControlGroupPercent() {
        return controlGroupPercent;
    }

    /**
     * 设置ControlGroupPercent。
     */
    public void setControlGroupPercent(Integer controlGroupPercent) {
        this.controlGroupPercent = controlGroupPercent;
    }

    /**
     * 获取ControlGroupSalt。
     */
    public String getControlGroupSalt() {
        return controlGroupSalt;
    }

    /**
     * 设置ControlGroupSalt。
     */
    public void setControlGroupSalt(String controlGroupSalt) {
        this.controlGroupSalt = controlGroupSalt;
    }

    /**
     * 获取ConversionEventCode。
     */
    public String getConversionEventCode() {
        return conversionEventCode;
    }

    /**
     * 设置ConversionEventCode。
     */
    public void setConversionEventCode(String conversionEventCode) {
        this.conversionEventCode = conversionEventCode;
    }

    /**
     * 获取AttributionWindowDays。
     */
    public Integer getAttributionWindowDays() {
        return attributionWindowDays;
    }

    /**
     * 设置AttributionWindowDays。
     */
    public void setAttributionWindowDays(Integer attributionWindowDays) {
        this.attributionWindowDays = attributionWindowDays;
    }

    /**
     * 获取AttributionModel。
     */
    public String getAttributionModel() {
        return attributionModel;
    }

    /**
     * 设置AttributionModel。
     */
    public void setAttributionModel(String attributionModel) {
        this.attributionModel = attributionModel;
    }

    /**
     * 获取MaxTotalExecutions。
     */
    public Integer getMaxTotalExecutions() {
        return maxTotalExecutions;
    }

    /**
     * 设置MaxTotalExecutions。
     */
    public void setMaxTotalExecutions(Integer maxTotalExecutions) {
        this.maxTotalExecutions = maxTotalExecutions;
    }

    /**
     * 获取TriggerType。
     */
    public String getTriggerType() {
        return triggerType;
    }

    /**
     * 设置TriggerType。
     */
    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    /**
     * 获取CronExpression。
     */
    public String getCronExpression() {
        return cronExpression;
    }

    /**
     * 设置CronExpression。
     */
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    /**
     * 获取canary version标识。
     */
    public Long getCanaryVersionId() {
        return canaryVersionId;
    }

    /**
     * 设置canary version标识。
     */
    public void setCanaryVersionId(Long canaryVersionId) {
        this.canaryVersionId = canaryVersionId;
    }

    /**
     * 获取CanaryPercent。
     */
    public Integer getCanaryPercent() {
        return canaryPercent;
    }

    /**
     * 设置CanaryPercent。
     */
    public void setCanaryPercent(Integer canaryPercent) {
        this.canaryPercent = canaryPercent;
    }

    /**
     * 获取previous version标识。
     */
    public Long getPreviousVersionId() {
        return previousVersionId;
    }

    /**
     * 设置previous version标识。
     */
    public void setPreviousVersionId(Long previousVersionId) {
        this.previousVersionId = previousVersionId;
    }
}
