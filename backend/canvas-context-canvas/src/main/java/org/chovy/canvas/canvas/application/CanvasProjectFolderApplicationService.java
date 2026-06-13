package org.chovy.canvas.canvas.application;

import org.springframework.stereotype.Service;

@Service
public class CanvasProjectFolderApplicationService {

    private final CanvasProjectFolderRepository repository;

    public CanvasProjectFolderApplicationService(CanvasProjectFolderRepository repository) {
        this.repository = repository;
    }

    public ProjectFolderMetadata getMetadata(Long tenantId, Long canvasId) {
        return repository.findByTenantIdAndCanvasId(tenantId, canvasId)
                .orElse(new ProjectFolderMetadata(canvasId, null, null, null, null, null, null));
    }

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

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record SaveProjectFolderCommand(
            Long projectId,
            String projectKey,
            String projectName,
            String folderKey,
            String folderName,
            String operator) {
    }
}
