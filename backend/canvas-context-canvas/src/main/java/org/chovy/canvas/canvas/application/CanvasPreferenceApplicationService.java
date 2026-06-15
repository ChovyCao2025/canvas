package org.chovy.canvas.canvas.application;

import java.util.Map;

import org.chovy.canvas.canvas.api.CanvasPreferenceFacade;
import org.chovy.canvas.canvas.domain.CanvasPreferenceCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CanvasPreferenceApplicationService implements CanvasPreferenceFacade {

    private final CanvasPreferenceCatalog catalog;

    public CanvasPreferenceApplicationService() {
        this(new CanvasPreferenceCatalog());
    }

    CanvasPreferenceApplicationService(CanvasPreferenceCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public PreferenceView getEditorPreference(Long tenantId, String userId) {
        return catalog.getEditorPreference(safeTenantId(tenantId), userOrDefault(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PreferenceView upsertEditorPreference(Long tenantId, String userId, Map<String, Object> patch) {
        return catalog.upsertEditorPreference(safeTenantId(tenantId), userOrDefault(userId), patch);
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static String userOrDefault(String userId) {
        return userId == null || userId.isBlank() ? "system" : userId.trim();
    }
}
