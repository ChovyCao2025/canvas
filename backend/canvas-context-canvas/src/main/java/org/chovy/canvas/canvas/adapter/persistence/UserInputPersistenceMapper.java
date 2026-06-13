package org.chovy.canvas.canvas.adapter.persistence;

import org.chovy.canvas.canvas.domain.UserInputForm;
import org.chovy.canvas.canvas.domain.UserInputResponse;
import org.chovy.canvas.canvas.domain.UserInputStatus;

final class UserInputPersistenceMapper {

    private UserInputPersistenceMapper() {
    }

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
