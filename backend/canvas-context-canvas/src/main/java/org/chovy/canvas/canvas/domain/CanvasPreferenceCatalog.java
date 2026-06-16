package org.chovy.canvas.canvas.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.chovy.canvas.canvas.api.CanvasPreferenceFacade.PreferenceView;

/**
 * 封装CanvasPreferenceCatalog相关的业务逻辑。
 */
public class CanvasPreferenceCatalog {

    /**
     * 保存EDITOR_KEY。
     */
    private static final String EDITOR_KEY = "canvas-editor";
    private static final Set<String> ALLOWED_EDITOR_KEYS = Set.of(
            "theme", "sidebarCollapsed", "notificationLevel", "recentNodeTypes", "editorLayout", "listDefaults");

    /**
     * 保存内存实现使用的preferences映射数据。
     */
    private final Map<Key, Map<String, Object>> preferences = new LinkedHashMap<>();

    /**
     * 获取EditorPreference。
     */
    public synchronized PreferenceView getEditorPreference(Long tenantId, String userId) {
        Map<String, Object> stored = preferences.get(new Key(tenantId, userId, EDITOR_KEY));
        return new PreferenceView(EDITOR_KEY, stored == null ? defaultEditorPreferences() : stored);
    }

    /**
     * 新增或更新编辑器偏好配置。
     */
    public synchronized PreferenceView upsertEditorPreference(Long tenantId,
                                                              String userId,
                                                              Map<String, Object> patch) {
        Map<String, Object> safePatch = patch == null ? Map.of() : patch;
        for (String key : safePatch.keySet()) {
            if (!ALLOWED_EDITOR_KEYS.contains(key)) {
                throw new IllegalArgumentException("unsupported preference key " + key);
            }
        }
        Map<String, Object> merged = new LinkedHashMap<>(getEditorPreference(tenantId, userId).preferenceJson());
        merged.putAll(safePatch);
        preferences.put(new Key(tenantId, userId, EDITOR_KEY), merged);
        return new PreferenceView(EDITOR_KEY, merged);
    }

    /**
     * 处理defaultEditorPreferences。
     */
    private static Map<String, Object> defaultEditorPreferences() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("theme", "system");
        defaults.put("sidebarCollapsed", false);
        defaults.put("notificationLevel", "mentions");
        defaults.put("recentNodeTypes", List.of());
        defaults.put("editorLayout", "default");
        defaults.put("listDefaults", Map.of("pageSize", 20));
        return defaults;
    }

    /**
     * 承载键的数据快照。
     */
    private record Key(Long tenantId, String userId, String preferenceKey) {
    }
}
