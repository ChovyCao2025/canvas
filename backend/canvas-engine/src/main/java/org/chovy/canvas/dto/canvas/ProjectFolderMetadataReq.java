package org.chovy.canvas.dto.canvas;

/**
 * ProjectFolderMetadataReq 承载 dto.canvas 场景中的不可变数据快照。
 * @param projectId projectId 字段。
 * @param projectKey projectKey 字段。
 * @param projectName projectName 字段。
 * @param folderKey folderKey 字段。
 * @param folderName folderName 字段。
 * @param operator operator 字段。
 */
public record ProjectFolderMetadataReq(
        Long projectId,
        String projectKey,
        String projectName,
        String folderKey,
        String folderName,
        String operator
) {
}
