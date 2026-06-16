package org.chovy.canvas.bi.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
/**
 * BiResourcePermissionDO 持久化对象。
 */
@TableName("bi_resource_permission")
public class BiResourcePermissionDO {
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
     * 资源类型。
     */
    private String resourceType;

    /**
     * 资源标识。
     */
    private Long resourceId;

    /**
     * subjectType 字段值。
     */
    private String subjectType;

    /**
     * subjectId 对应的标识。
     */
    private String subjectId;

    /**
     * actionKey 对应的业务键。
     */
    private String actionKey;

    /**
     * effect 字段值。
     */
    private String effect;

    /**
     * 创建人。
     */
    private String createdBy;

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
     * 获取 Resource Type。
     */
    public String getResourceType() {
        return resourceType;
    }
    /**
     * 设置 Resource Type。
     */
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
    /**
     * 获取 Resource Id。
     */
    public Long getResourceId() {
        return resourceId;
    }
    /**
     * 设置 Resource Id。
     */
    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }
    /**
     * 获取 Subject Type。
     */
    public String getSubjectType() {
        return subjectType;
    }
    /**
     * 设置 Subject Type。
     */
    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }
    /**
     * 获取 Subject Id。
     */
    public String getSubjectId() {
        return subjectId;
    }
    /**
     * 设置 Subject Id。
     */
    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }
    /**
     * 获取 Action Key。
     */
    public String getActionKey() {
        return actionKey;
    }
    /**
     * 设置 Action Key。
     */
    public void setActionKey(String actionKey) {
        this.actionKey = actionKey;
    }
    /**
     * 获取 Effect。
     */
    public String getEffect() {
        return effect;
    }
    /**
     * 设置 Effect。
     */
    public void setEffect(String effect) {
        this.effect = effect;
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
}
