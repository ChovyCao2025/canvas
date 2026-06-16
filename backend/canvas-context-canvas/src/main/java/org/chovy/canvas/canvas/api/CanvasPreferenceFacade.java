package org.chovy.canvas.canvas.api;

import java.util.Map;

/**
 * 定义CanvasPreferenceFacade对外提供的能力契约。
 */
public interface CanvasPreferenceFacade {

    /**
     * 获取EditorPreference。
     */
    PreferenceView getEditorPreference(Long tenantId, String userId);

    /**
     * 处理upsertEditorPreference。
     */
    PreferenceView upsertEditorPreference(Long tenantId, String userId, Map<String, Object> patch);

    /**
     * 承载PreferenceView的数据快照。
     */
    record PreferenceView(String preferenceKey, Map<String, Object> preferenceJson) {
        public PreferenceView {
            preferenceJson = preferenceJson == null ? Map.of() : Map.copyOf(preferenceJson);
        }
    }
}
