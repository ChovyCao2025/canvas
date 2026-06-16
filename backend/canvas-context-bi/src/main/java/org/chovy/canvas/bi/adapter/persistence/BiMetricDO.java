package org.chovy.canvas.bi.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
/**
 * BiMetricDO 持久化对象。
 */
@TableName("bi_metric")
public class BiMetricDO {
    /**
     * 唯一标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户标识。
     */
    private Long tenantId;

    /**
     * 工作空间标识。
     */
    private Long workspaceId;

    /**
     * 数据集标识。
     */
    private Long datasetId;

    /**
     * 指标键。
     */
    private String metricKey;

    /**
     * displayName 字段值。
     */
    private String displayName;

    /**
     * expression 字段值。
     */
    private String expression;

    /**
     * aggregation 字段值。
     */
    private String aggregation;

    /**
     * dataType 字段值。
     */
    private String dataType;

    /**
     * unit 字段值。
     */
    private String unit;

    /**
     * 状态值。
     */
    private String status;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;

    /**
     * 获取 Id。
     */
    public Long getId() {
        return id;
    }
    /**
     * 设置 Id。
     */
    public void setId(Long id) {
        this.id = id;
    }
    /**
     * 获取 Tenant Id。
     */
    public Long getTenantId() {
        return tenantId;
    }
    /**
     * 设置 Tenant Id。
     */
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
    /**
     * 获取 Workspace Id。
     */
    public Long getWorkspaceId() {
        return workspaceId;
    }
    /**
     * 设置 Workspace Id。
     */
    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }
    /**
     * 获取 Dataset Id。
     */
    public Long getDatasetId() {
        return datasetId;
    }
    /**
     * 设置 Dataset Id。
     */
    public void setDatasetId(Long datasetId) {
        this.datasetId = datasetId;
    }
    /**
     * 获取 Metric Key。
     */
    public String getMetricKey() {
        return metricKey;
    }
    /**
     * 设置 Metric Key。
     */
    public void setMetricKey(String metricKey) {
        this.metricKey = metricKey;
    }
    /**
     * 获取 Display Name。
     */
    public String getDisplayName() {
        return displayName;
    }
    /**
     * 设置 Display Name。
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    /**
     * 获取 Expression。
     */
    public String getExpression() {
        return expression;
    }
    /**
     * 设置 Expression。
     */
    public void setExpression(String expression) {
        this.expression = expression;
    }
    /**
     * 获取 Aggregation。
     */
    public String getAggregation() {
        return aggregation;
    }
    /**
     * 设置 Aggregation。
     */
    public void setAggregation(String aggregation) {
        this.aggregation = aggregation;
    }
    /**
     * 获取 Data Type。
     */
    public String getDataType() {
        return dataType;
    }
    /**
     * 设置 Data Type。
     */
    public void setDataType(String dataType) {
        this.dataType = dataType;
    }
    /**
     * 获取 Unit。
     */
    public String getUnit() {
        return unit;
    }
    /**
     * 设置 Unit。
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }
    /**
     * 获取 Status。
     */
    public String getStatus() {
        return status;
    }
    /**
     * 设置 Status。
     */
    public void setStatus(String status) {
        this.status = status;
    }
    /**
     * 获取 Created At。
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    /**
     * 设置 Created At。
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    /**
     * 获取 Updated At。
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    /**
     * 设置 Updated At。
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
