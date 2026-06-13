package org.chovy.canvas.canvas.application;

import java.util.Optional;

public interface CanvasProjectFolderRepository {

    Optional<ProjectFolderMetadata> findByTenantIdAndCanvasId(Long tenantId, Long canvasId);

    ProjectFolderMetadata save(ProjectFolderMetadata metadata);
}
