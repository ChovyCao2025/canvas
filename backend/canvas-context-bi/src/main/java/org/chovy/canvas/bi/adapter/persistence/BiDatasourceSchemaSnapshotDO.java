package org.chovy.canvas.bi.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
/**
 * BiDatasourceSchemaSnapshotDO 持久化对象。
 */
@TableName("bi_datasource_schema_snapshot")
public class BiDatasourceSchemaSnapshotDO {
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
     * dataSourceConfigId 对应的标识。
     */
    private Long dataSourceConfigId;

    /**
     * sourceKey 对应的业务键。
     */
    private String sourceKey;

    /**
     * connectorType 字段值。
     */
    private String connectorType;

    /**
     * schemaJson 的 JSON 序列化内容。
     */
    private String schemaJson;

    /**
     * syncStatus 对应的数据集合。
     */
    private String syncStatus;

    /**
     * errorMessage 字段值。
     */
    private String errorMessage;

    /**
     * tableCount 对应的统计数量。
     */
    private Integer tableCount;

    /**
     * columnCount 对应的统计数量。
     */
    private Integer columnCount;

    /**
     * syncedBy 字段值。
     */
    private String syncedBy;

    /**
     * syncedAt 对应的时间。
     */
    private LocalDateTime syncedAt;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

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
     * 获取 Data Source Config Id。
     */
    public Long getDataSourceConfigId() {
        return dataSourceConfigId;
    }
    /**
     * 设置 Data Source Config Id。
     */
    public void setDataSourceConfigId(Long dataSourceConfigId) {
        this.dataSourceConfigId = dataSourceConfigId;
    }
    /**
     * 获取 Source Key。
     */
    public String getSourceKey() {
        return sourceKey;
    }
    /**
     * 设置 Source Key。
     */
    public void setSourceKey(String sourceKey) {
        this.sourceKey = sourceKey;
    }
    /**
     * 获取 Connector Type。
     */
    public String getConnectorType() {
        return connectorType;
    }
    /**
     * 设置 Connector Type。
     */
    public void setConnectorType(String connectorType) {
        this.connectorType = connectorType;
    }
    /**
     * 获取 Schema Json。
     */
    public String getSchemaJson() {
        return schemaJson;
    }
    /**
     * 设置 Schema Json。
     */
    public void setSchemaJson(String schemaJson) {
        this.schemaJson = schemaJson;
    }
    /**
     * 获取 Sync Status。
     */
    public String getSyncStatus() {
        return syncStatus;
    }
    /**
     * 设置 Sync Status。
     */
    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }
    /**
     * 获取 Error Message。
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    /**
     * 设置 Error Message。
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    /**
     * 获取 Table Count。
     */
    public Integer getTableCount() {
        return tableCount;
    }
    /**
     * 设置 Table Count。
     */
    public void setTableCount(Integer tableCount) {
        this.tableCount = tableCount;
    }
    /**
     * 获取 Column Count。
     */
    public Integer getColumnCount() {
        return columnCount;
    }
    /**
     * 设置 Column Count。
     */
    public void setColumnCount(Integer columnCount) {
        this.columnCount = columnCount;
    }
    /**
     * 获取 Synced By。
     */
    public String getSyncedBy() {
        return syncedBy;
    }
    /**
     * 设置 Synced By。
     */
    public void setSyncedBy(String syncedBy) {
        this.syncedBy = syncedBy;
    }
    /**
     * 获取 Synced At。
     */
    public LocalDateTime getSyncedAt() {
        return syncedAt;
    }
    /**
     * 设置 Synced At。
     */
    public void setSyncedAt(LocalDateTime syncedAt) {
        this.syncedAt = syncedAt;
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
}
