package org.chovy.canvas.bi.domain;
/**
 * BiWorkspaceRepository 仓储接口。
 */
public interface BiWorkspaceRepository {
    /**
     * 执行 find Workspace 相关处理。
     */

    BiWorkspace findWorkspace(Long tenantId, Long workspaceId);
    /**
     * 执行 save Workspace 相关处理。
     */

    BiWorkspace saveWorkspace(BiWorkspace workspace);
}
