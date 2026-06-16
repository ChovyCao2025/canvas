package org.chovy.canvas.bi.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
/**
 * BiDatasetFieldDO 持久化对象。
 */
@TableName("bi_dataset_field")
public class BiDatasetFieldDO {
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
     * 数据集标识。
     */
    private Long datasetId;

    /**
     * fieldKey 对应的业务键。
     */
    private String fieldKey;

    /**
     * displayName 字段值。
     */
    private String displayName;

    /**
     * columnExpression 字段值。
     */
    private String columnExpression;

    /**
     * roleKey 对应的业务键。
     */
    private String roleKey;

    /**
     * dataType 字段值。
     */
    private String dataType;

    /**
     * defaultAggregation 字段值。
     */
    private String defaultAggregation;

    /**
     * visible 字段值。
     */
    private Boolean visible;

    /**
     * sortOrder 字段值。
     */
    private Integer sortOrder;

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
     * 获取 Field Key。
     */
    public String getFieldKey() {
        return fieldKey;
    }
    /**
     * 设置 Field Key。
     */
    public void setFieldKey(String fieldKey) {
        this.fieldKey = fieldKey;
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
     * 获取 Column Expression。
     */
    public String getColumnExpression() {
        return columnExpression;
    }
    /**
     * 设置 Column Expression。
     */
    public void setColumnExpression(String columnExpression) {
        this.columnExpression = columnExpression;
    }
    /**
     * 获取 Role Key。
     */
    public String getRoleKey() {
        return roleKey;
    }
    /**
     * 设置 Role Key。
     */
    public void setRoleKey(String roleKey) {
        this.roleKey = roleKey;
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
     * 获取 Default Aggregation。
     */
    public String getDefaultAggregation() {
        return defaultAggregation;
    }
    /**
     * 设置 Default Aggregation。
     */
    public void setDefaultAggregation(String defaultAggregation) {
        this.defaultAggregation = defaultAggregation;
    }
    /**
     * 获取 Visible。
     */
    public Boolean getVisible() {
        return visible;
    }
    /**
     * 设置 Visible。
     */
    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
    /**
     * 获取 Sort Order。
     */
    public Integer getSortOrder() {
        return sortOrder;
    }
    /**
     * 设置 Sort Order。
     */
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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
