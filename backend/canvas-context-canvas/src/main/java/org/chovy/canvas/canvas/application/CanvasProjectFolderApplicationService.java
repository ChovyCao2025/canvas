package org.chovy.canvas.canvas.application;

import org.springframework.stereotype.Service;

/**
 * 封装CanvasProjectFolderApplicationService相关的业务逻辑。
 */
@Service
public class CanvasProjectFolderApplicationService {

    /**
     * 保存仓储。
     */
    private final CanvasProjectFolderRepository repository;

    /**
     * 创建当前对象实例。
     */
    public CanvasProjectFolderApplicationService(CanvasProjectFolderRepository repository) {
        this.repository = repository;
    }

    /**
     * 获取元数据。
     */
    public ProjectFolderMetadata getMetadata(Long tenantId, Long canvasId) {
        return repository.findByTenantIdAndCanvasId(tenantId, canvasId)
                .orElse(new ProjectFolderMetadata(canvasId, null, null, null, null, null, null));
    }

    /**
     * 保存元数据。
     */
    public ProjectFolderMetadata saveMetadata(Long tenantId, Long canvasId, SaveProjectFolderCommand command) {
        ProjectFolderMetadata metadata = new ProjectFolderMetadata(
                canvasId,
                tenantId,
                command.projectId(),
                normalize(command.projectKey()),
                normalize(command.projectName()),
                normalize(command.folderKey()),
                normalize(command.folderName()));
        return repository.save(metadata);
    }

    /**
     * 规范化。
     */
    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 承载SaveProjectFolderCommand的数据快照。
     */
    public record SaveProjectFolderCommand(
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
            String folderName,
            /**
             * 记录操作人。
             */
            String operator) {
    }
}
