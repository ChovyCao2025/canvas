package org.chovy.canvas.canvas.api;

import java.util.Map;

public interface CanvasPreferenceFacade {

    PreferenceView getEditorPreference(Long tenantId, String userId);

    PreferenceView upsertEditorPreference(Long tenantId, String userId, Map<String, Object> patch);

    record PreferenceView(String preferenceKey, Map<String, Object> preferenceJson) {
        public PreferenceView {
            preferenceJson = preferenceJson == null ? Map.of() : Map.copyOf(preferenceJson);
        }
    }
}
