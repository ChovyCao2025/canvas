package org.chovy.canvas.bi.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
/**
 * BiDashboardDO 持久化对象。
 */
@TableName("bi_dashboard")
public class BiDashboardDO {
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
     * 仪表盘键。
     */
    private String dashboardKey;

    /**
     * 展示名称。
     */
    private String name;

    /**
     * 说明文本。
     */
    private String description;

    /**
     * themeJson 的 JSON 序列化内容。
     */
    private String themeJson;

    /**
     * filterJson 的 JSON 序列化内容。
     */
    private String filterJson;

    /**
     * 状态值。
     */
    private String status;

    /**
     * 版本号。
     */
    private Integer version;

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
     * 获取 Dashboard Key。
     */
    public String getDashboardKey() {
        return dashboardKey;
    }
    /**
     * 设置 Dashboard Key。
     */
    public void setDashboardKey(String dashboardKey) {
        this.dashboardKey = dashboardKey;
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
     * 获取 Theme Json。
     */
    public String getThemeJson() {
        return themeJson;
    }
    /**
     * 设置 Theme Json。
     */
    public void setThemeJson(String themeJson) {
        this.themeJson = themeJson;
    }
    /**
     * 获取 Filter Json。
     */
    public String getFilterJson() {
        return filterJson;
    }
    /**
     * 设置 Filter Json。
     */
    public void setFilterJson(String filterJson) {
        this.filterJson = filterJson;
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
     * 获取 Version。
     */
    public Integer getVersion() {
        return version;
    }
    /**
     * 设置 Version。
     */
    public void setVersion(Integer version) {
        this.version = version;
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
