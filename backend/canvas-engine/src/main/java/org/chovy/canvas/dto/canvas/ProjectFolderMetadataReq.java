package org.chovy.canvas.dto.canvas;

public record ProjectFolderMetadataReq(
        Long projectId,
        String projectKey,
        String projectName,
        String folderKey,
        String folderName,
        String operator
) {
}
