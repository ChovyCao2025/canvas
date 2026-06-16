package org.chovy.canvas.bi.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
/**
 * BiDatasetDO 持久化对象。
 */
@TableName("bi_dataset")
public class BiDatasetDO {
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
     * 数据集键。
     */
    private String datasetKey;

    /**
     * 展示名称。
     */
    private String name;

    /**
     * datasetType 字段值。
     */
    private String datasetType;

    /**
     * sourceRefId 对应的标识。
     */
    private Long sourceRefId;

    /**
     * tableExpression 字段值。
     */
    private String tableExpression;

    /**
     * tenantColumn 字段值。
     */
    private String tenantColumn;

    /**
     * modelJson 的 JSON 序列化内容。
     */
    private String modelJson;

    /**
     * 状态值。
     */
    private String status;

    /**
     * 创建人。
     */
    private String createdBy;

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
     * 获取 Dataset Key。
     */
    public String getDatasetKey() {
        return datasetKey;
    }
    /**
     * 设置 Dataset Key。
     */
    public void setDatasetKey(String datasetKey) {
        this.datasetKey = datasetKey;
    }
    /**
     * 获取 Name。
     */
    public String getName() {
        return name;
    }
    /**
     * 设置 Name。
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * 获取 Dataset Type。
     */
    public String getDatasetType() {
        return datasetType;
    }
    /**
     * 设置 Dataset Type。
     */
    public void setDatasetType(String datasetType) {
        this.datasetType = datasetType;
    }
    /**
     * 获取 Source Ref Id。
     */
    public Long getSourceRefId() {
        return sourceRefId;
    }
    /**
     * 设置 Source Ref Id。
     */
    public void setSourceRefId(Long sourceRefId) {
        this.sourceRefId = sourceRefId;
    }
    /**
     * 获取 Table Expression。
     */
    public String getTableExpression() {
        return tableExpression;
    }
    /**
     * 设置 Table Expression。
     */
    public void setTableExpression(String tableExpression) {
        this.tableExpression = tableExpression;
    }
    /**
     * 获取 Tenant Column。
     */
    public String getTenantColumn() {
        return tenantColumn;
    }
    /**
     * 设置 Tenant Column。
     */
    public void setTenantColumn(String tenantColumn) {
        this.tenantColumn = tenantColumn;
    }
    /**
     * 获取 Model Json。
     */
    public String getModelJson() {
        return modelJson;
    }
    /**
     * 设置 Model Json。
     */
    public void setModelJson(String modelJson) {
        this.modelJson = modelJson;
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
     * 获取 Created By。
     */
    public String getCreatedBy() {
        return createdBy;
    }
    /**
     * 设置 Created By。
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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
