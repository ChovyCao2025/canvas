package org.chovy.canvas.canvas.application;

public record ProjectFolderMetadata(
        Long canvasId,
        Long tenantId,
        Long projectId,
        String projectKey,
        String projectName,
        String folderKey,
        String folderName) {
}
