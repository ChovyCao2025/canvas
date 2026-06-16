package org.chovy.canvas.bi.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
/**
 * BiChartDO 持久化对象。
 */
@TableName("bi_chart")
public class BiChartDO {
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
     * 图表键。
     */
    private String chartKey;

    /**
     * 展示名称。
     */
    private String name;

    /**
     * chartType 字段值。
     */
    private String chartType;

    /**
     * 数据集标识。
     */
    private Long datasetId;

    /**
     * queryJson 的 JSON 序列化内容。
     */
    private String queryJson;

    /**
     * styleJson 的 JSON 序列化内容。
     */
    private String styleJson;

    /**
     * interactionJson 的 JSON 序列化内容。
     */
    private String interactionJson;

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
     * 获取 Chart Key。
     */
    public String getChartKey() {
        return chartKey;
    }
    /**
     * 设置 Chart Key。
     */
    public void setChartKey(String chartKey) {
        this.chartKey = chartKey;
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
     * 获取 Chart Type。
     */
    public String getChartType() {
        return chartType;
    }
    /**
     * 设置 Chart Type。
     */
    public void setChartType(String chartType) {
        this.chartType = chartType;
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
     * 获取 Query Json。
     */
    public String getQueryJson() {
        return queryJson;
    }
    /**
     * 设置 Query Json。
     */
    public void setQueryJson(String queryJson) {
        this.queryJson = queryJson;
    }
    /**
     * 获取 Style Json。
     */
    public String getStyleJson() {
        return styleJson;
    }
    /**
     * 设置 Style Json。
     */
    public void setStyleJson(String styleJson) {
        this.styleJson = styleJson;
    }
    /**
     * 获取 Interaction Json。
     */
    public String getInteractionJson() {
        return interactionJson;
    }
    /**
     * 设置 Interaction Json。
     */
    public void setInteractionJson(String interactionJson) {
        this.interactionJson = interactionJson;
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
