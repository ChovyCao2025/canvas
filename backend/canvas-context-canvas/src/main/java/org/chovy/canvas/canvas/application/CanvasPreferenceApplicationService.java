package org.chovy.canvas.canvas.application;

import java.util.Map;

import org.chovy.canvas.canvas.api.CanvasPreferenceFacade;
import org.chovy.canvas.canvas.domain.CanvasPreferenceCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 封装CanvasPreferenceApplicationService相关的业务逻辑。
 */
@Service
public class CanvasPreferenceApplicationService implements CanvasPreferenceFacade {

    /**
     * 保存catalog。
     */
    private final CanvasPreferenceCatalog catalog;

    /**
     * 创建当前对象实例。
     */
    public CanvasPreferenceApplicationService() {
        this(new CanvasPreferenceCatalog());
    }

    /**
     * 创建当前对象实例。
     */
    CanvasPreferenceApplicationService(CanvasPreferenceCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 获取EditorPreference。
     */
    @Override
    public PreferenceView getEditorPreference(Long tenantId, String userId) {
        return catalog.getEditorPreference(safeTenantId(tenantId), userOrDefault(userId));
    }

    /**
     * 处理upsertEditorPreference。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PreferenceView upsertEditorPreference(Long tenantId, String userId, Map<String, Object> patch) {
        return catalog.upsertEditorPreference(safeTenantId(tenantId), userOrDefault(userId), patch);
    }

    /**
     * 处理safe tenant标识。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 处理userOrDefault。
     */
    private static String userOrDefault(String userId) {
        return userId == null || userId.isBlank() ? "system" : userId.trim();
    }
}
