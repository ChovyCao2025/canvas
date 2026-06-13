package org.chovy.canvas.bi.domain;

import java.util.List;

public interface BiPermissionRepository {

    BiPermissionGrant saveGrant(BiPermissionGrant grant);

    List<BiPermissionGrant> listResourceGrants(Long tenantId, Long workspaceId, String resourceType, Long resourceId);
}
