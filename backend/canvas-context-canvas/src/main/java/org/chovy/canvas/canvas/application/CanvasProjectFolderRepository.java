package org.chovy.canvas.canvas.application;

import java.util.Optional;

/**
 * 定义CanvasProjectFolderRepository对外提供的能力契约。
 */
public interface CanvasProjectFolderRepository {

    /**
     * 查询by tenant id and canvas标识。
     */
    Optional<ProjectFolderMetadata> findByTenantIdAndCanvasId(Long tenantId, Long canvasId);

    /**
     * 保存。
     */
    ProjectFolderMetadata save(ProjectFolderMetadata metadata);
}
