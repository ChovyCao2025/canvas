package org.chovy.canvas.canvas.application;

/**
 * 承载ProjectFolderMetadata的数据快照。
 */
public record ProjectFolderMetadata(
        /**
         * 记录画布标识。
         */
        Long canvasId,
        /**
         * 记录租户标识。
         */
        Long tenantId,
        /**
         * 记录project标识。
         */
        Long projectId,
        /**
         * 记录projectKey。
         */
        String projectKey,
        /**
         * 记录projectName。
         */
        String projectName,
        /**
         * 记录folderKey。
         */
        String folderKey,
        /**
         * 记录folderName。
         */
        String folderName) {
}
