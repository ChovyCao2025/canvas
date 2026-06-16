package org.chovy.canvas.canvas.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 封装UserInputResponseDO相关的业务逻辑。
 */
@TableName("user_input_response")
public class UserInputResponseDO {

    /**
     * 保存标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 保存租户标识。
     */
    private Long tenantId;

    /**
     * 保存form标识。
     */
    private Long formId;

    /**
     * 保存画布标识。
     */
    private Long canvasId;

    /**
     * 保存版本标识。
     */
    private Long versionId;

    /**
     * 保存execution标识。
     */
    private String executionId;

    /**
     * 保存节点标识。
     */
    private String nodeId;

    /**
     * 保存用户标识。
     */
    private String userId;

    /**
     * 保存响应JSON 内容。
     */
    private String responseJson;

    /**
     * 保存状态。
     */
    private String status;

    /**
     * 保存idempotencyKey。
     */
    private String idempotencyKey;

    /**
     * 保存completed node标识。
     */
    private String completedNodeId;

    /**
     * 保存timeout node标识。
     */
    private String timeoutNodeId;

    /**
     * 保存expires时间。
     */
    private LocalDateTime expiresAt;

    /**
     * 保存创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 保存更新时间。
     */
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
     * 获取form标识。
     */
    public Long getFormId() {
        return formId;
    }

    /**
     * 设置form标识。
     */
    public void setFormId(Long formId) {
        this.formId = formId;
    }

    /**
     * 获取画布标识。
     */
    public Long getCanvasId() {
        return canvasId;
    }

    /**
     * 设置画布标识。
     */
    public void setCanvasId(Long canvasId) {
        this.canvasId = canvasId;
    }

    /**
     * 获取版本标识。
     */
    public Long getVersionId() {
        return versionId;
    }

    /**
     * 设置版本标识。
     */
    public void setVersionId(Long versionId) {
        this.versionId = versionId;
    }

    /**
     * 获取execution标识。
     */
    public String getExecutionId() {
        return executionId;
    }

    /**
     * 设置execution标识。
     */
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    /**
     * 获取节点标识。
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * 设置节点标识。
     */
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * 获取用户标识。
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 设置用户标识。
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * 获取响应JSON 内容。
     */
    public String getResponseJson() {
        return responseJson;
    }

    /**
     * 设置响应JSON 内容。
     */
    public void setResponseJson(String responseJson) {
        this.responseJson = responseJson;
    }

    /**
     * 获取状态。
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置状态。
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取IdempotencyKey。
     */
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /**
     * 设置IdempotencyKey。
     */
    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    /**
     * 获取completed node标识。
     */
    public String getCompletedNodeId() {
        return completedNodeId;
    }

    /**
     * 设置completed node标识。
     */
    public void setCompletedNodeId(String completedNodeId) {
        this.completedNodeId = completedNodeId;
    }

    /**
     * 获取timeout node标识。
     */
    public String getTimeoutNodeId() {
        return timeoutNodeId;
    }

    /**
     * 设置timeout node标识。
     */
    public void setTimeoutNodeId(String timeoutNodeId) {
        this.timeoutNodeId = timeoutNodeId;
    }

    /**
     * 获取expires时间。
     */
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    /**
     * 设置expires时间。
     */
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    /**
     * 获取创建时间。
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间。
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取更新时间。
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置更新时间。
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
