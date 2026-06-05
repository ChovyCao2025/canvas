package org.chovy.canvas.domain.bi.resource;

import java.time.LocalDateTime;

public record BiResourceFavoriteView(
        Long id,
        Long tenantId,
        Long workspaceId,
        String resourceType,
        String resourceKey,
        String username,
        Boolean favorite,
        LocalDateTime createdAt) {

    public BiResourceFavoriteView(Long id,
                                  Long tenantId,
                                  Long workspaceId,
                                  String resourceType,
                                  String resourceKey,
                                  String username,
                                  LocalDateTime createdAt) {
        this(id, tenantId, workspaceId, resourceType, resourceKey, username, true, createdAt);
    }
}
