package org.chovy.canvas.canvas.adapter.persistence;

import org.chovy.canvas.canvas.domain.UserInputForm;
import org.chovy.canvas.canvas.domain.UserInputResponse;
import org.chovy.canvas.canvas.domain.UserInputStatus;

/**
 * 封装UserInputPersistenceMapper相关的业务逻辑。
 */
final class UserInputPersistenceMapper {

    /**
     * 创建当前对象实例。
     */
    private UserInputPersistenceMapper() {
    }

    /**
     * 转换为FormRow。
     */
    static UserInputFormDO toFormRow(UserInputForm form) {
        UserInputFormDO row = new UserInputFormDO();
        row.setId(form.id());
        row.setTenantId(form.tenantId());
        row.setCanvasId(form.canvasId());
        row.setVersionId(form.versionId());
        row.setExecutionId(form.executionId());
        row.setNodeId(form.nodeId());
        row.setUserId(form.userId());
        row.setSchemaJson(form.schemaJson());
        row.setCompletedNodeId(form.completedNodeId());
        row.setTimeoutNodeId(form.timeoutNodeId());
        row.setExpiresAt(form.expiresAt());
        row.setCreatedAt(form.createdAt());
        row.setUpdatedAt(form.updatedAt());
        return row;
    }

    /**
     * 转换为FormDomain。
     */
    static UserInputForm toFormDomain(UserInputFormDO row) {
        if (row == null) {
            return null;
        }
        return new UserInputForm(
                row.getId(),
                row.getTenantId(),
                row.getCanvasId(),
                row.getVersionId(),
                row.getExecutionId(),
                row.getNodeId(),
                row.getUserId(),
                row.getSchemaJson(),
                row.getCompletedNodeId(),
                row.getTimeoutNodeId(),
                row.getExpiresAt(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 转换为ResponseRow。
     */
    static UserInputResponseDO toResponseRow(UserInputResponse response) {
        UserInputResponseDO row = new UserInputResponseDO();
        row.setId(response.id());
        row.setTenantId(response.tenantId());
        row.setFormId(response.formId());
        row.setCanvasId(response.canvasId());
        row.setVersionId(response.versionId());
        row.setExecutionId(response.executionId());
        row.setNodeId(response.nodeId());
        row.setUserId(response.userId());
        row.setResponseJson(response.responseJson());
        row.setStatus(response.status().name());
        row.setIdempotencyKey(response.idempotencyKey());
        row.setCompletedNodeId(response.completedNodeId());
        row.setTimeoutNodeId(response.timeoutNodeId());
        row.setExpiresAt(response.expiresAt());
        row.setCreatedAt(response.createdAt());
        row.setUpdatedAt(response.updatedAt());
        return row;
    }

    /**
     * 转换为ResponseDomain。
     */
    static UserInputResponse toResponseDomain(UserInputResponseDO row) {
        if (row == null) {
            return null;
        }
        return new UserInputResponse(
                row.getId(),
                row.getTenantId(),
                row.getFormId(),
                row.getCanvasId(),
                row.getVersionId(),
                row.getExecutionId(),
                row.getNodeId(),
                row.getUserId(),
                row.getResponseJson(),
                UserInputStatus.valueOf(row.getStatus()),
                row.getIdempotencyKey(),
                row.getCompletedNodeId(),
                row.getTimeoutNodeId(),
                row.getExpiresAt(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }
}
