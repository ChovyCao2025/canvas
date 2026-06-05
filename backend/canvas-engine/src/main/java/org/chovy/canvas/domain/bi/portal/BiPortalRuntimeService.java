package org.chovy.canvas.domain.bi.portal;

import org.chovy.canvas.domain.bi.permission.BiPermissionService;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BiPortalRuntimeService {

    private static final String STATUS_PUBLISHED = "PUBLISHED";

    private final BiPortalResourceService portalResourceService;
    private final BiPermissionService permissionService;

    public BiPortalRuntimeService(BiPortalResourceService portalResourceService,
                                  BiPermissionService permissionService) {
        this.portalResourceService = portalResourceService;
        this.permissionService = permissionService;
    }

    public List<BiPortalResource> listPublished(Long tenantId, BiQueryContext context) {
        return portalResourceService.list(tenantId).stream()
                .filter(this::published)
                .map(portal -> withVisibleMenus(portal, context))
                .toList();
    }

    public BiPortalResource getPublished(Long tenantId, String portalKey, BiQueryContext context) {
        BiPortalResource portal = portalResourceService.get(tenantId, portalKey);
        if (!published(portal)) {
            throw new IllegalArgumentException("BI portal is not published: " + portalKey);
        }
        return withVisibleMenus(portal, context);
    }

    private BiPortalResource withVisibleMenus(BiPortalResource portal, BiQueryContext context) {
        return new BiPortalResource(
                portal.portalKey(),
                portal.name(),
                portal.theme(),
                permissionService.visibleMenus(portal.menus(), context),
                portal.status(),
                portal.source());
    }

    private boolean published(BiPortalResource portal) {
        return portal != null && STATUS_PUBLISHED.equals(portal.status());
    }
}
