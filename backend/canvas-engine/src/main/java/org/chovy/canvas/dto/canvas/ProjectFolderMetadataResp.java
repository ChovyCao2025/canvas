package org.chovy.canvas.dto.canvas;

public record ProjectFolderMetadataResp(
        Long canvasId,
        String projectKey,
        String projectName,
        String folderKey,
        String folderName
) {
}
