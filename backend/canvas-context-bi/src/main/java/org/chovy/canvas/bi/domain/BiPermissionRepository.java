package org.chovy.canvas.bi.domain;

import java.util.List;
/**
 * BiPermissionRepository 仓储接口。
 */
public interface BiPermissionRepository {
    /**
     * 执行 save Grant 相关处理。
     */

    BiPermissionGrant saveGrant(BiPermissionGrant grant);

    void deleteGrant(Long tenantId,
                     Long workspaceId,
                     String resourceType,
                     Long resourceId,
                     String subjectType,
                     String subjectId,
                     String actionKey);
    /**
     * 查询列表数据。
     */

    List<BiPermissionGrant> listResourceGrants(Long tenantId, Long workspaceId, String resourceType, Long resourceId);
}
