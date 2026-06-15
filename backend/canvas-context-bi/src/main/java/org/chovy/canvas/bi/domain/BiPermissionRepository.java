package org.chovy.canvas.bi.domain;

import java.util.List;

public interface BiPermissionRepository {

    BiPermissionGrant saveGrant(BiPermissionGrant grant);

    void deleteGrant(Long tenantId,
                     Long workspaceId,
                     String resourceType,
                     Long resourceId,
                     String subjectType,
                     String subjectId,
                     String actionKey);

    List<BiPermissionGrant> listResourceGrants(Long tenantId, Long workspaceId, String resourceType, Long resourceId);
}
