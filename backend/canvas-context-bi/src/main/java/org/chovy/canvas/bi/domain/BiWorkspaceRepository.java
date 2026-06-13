package org.chovy.canvas.bi.domain;

public interface BiWorkspaceRepository {

    BiWorkspace findWorkspace(Long tenantId, Long workspaceId);

    BiWorkspace saveWorkspace(BiWorkspace workspace);
}
