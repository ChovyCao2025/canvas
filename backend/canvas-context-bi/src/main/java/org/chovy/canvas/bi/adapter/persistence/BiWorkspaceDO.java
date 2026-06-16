package org.chovy.canvas.bi.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
/**
 * BiWorkspaceDO 持久化对象。
 */
@TableName("bi_workspace")
public class BiWorkspaceDO {
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
     * 工作空间键。
     */
    private String workspaceKey;

    /**
     * 展示名称。
     */
    private String name;

    /**
     * 说明文本。
     */
    private String description;

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
     * 获取 Workspace Key。
     */
    public String getWorkspaceKey() {
        return workspaceKey;
    }
    /**
     * 设置 Workspace Key。
     */
    public void setWorkspaceKey(String workspaceKey) {
        this.workspaceKey = workspaceKey;
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
     * 获取 Description。
     */
    public String getDescription() {
        return description;
    }
    /**
     * 设置 Description。
     */
    public void setDescription(String description) {
        this.description = description;
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
