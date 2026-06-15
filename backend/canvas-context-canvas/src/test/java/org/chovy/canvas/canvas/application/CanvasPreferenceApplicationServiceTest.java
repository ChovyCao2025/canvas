package org.chovy.canvas.canvas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.canvas.api.CanvasPreferenceFacade;
import org.junit.jupiter.api.Test;

class CanvasPreferenceApplicationServiceTest {

    @Test
    void editorPreferencesDefaultMergeAndRemainTenantUserScoped() {
        CanvasPreferenceFacade service = new CanvasPreferenceApplicationService();

        CanvasPreferenceFacade.PreferenceView defaults = service.getEditorPreference(7L, "operator-1");
        CanvasPreferenceFacade.PreferenceView updated = service.upsertEditorPreference(7L, "operator-1", Map.of(
                "theme", "dark",
                "sidebarCollapsed", true,
                "recentNodeTypes", List.of("sms")));
        CanvasPreferenceFacade.PreferenceView otherUser = service.getEditorPreference(7L, "operator-2");
        CanvasPreferenceFacade.PreferenceView otherTenant = service.getEditorPreference(8L, "operator-1");

        assertThat(defaults.preferenceKey()).isEqualTo("canvas-editor");
        assertThat(defaults.preferenceJson())
                .containsEntry("theme", "system")
                .containsEntry("notificationLevel", "mentions")
                .containsEntry("editorLayout", "default")
                .containsEntry("listDefaults", Map.of("pageSize", 20));
        assertThat(updated.preferenceJson())
                .containsEntry("theme", "dark")
                .containsEntry("sidebarCollapsed", true)
                .containsEntry("notificationLevel", "mentions")
                .containsEntry("recentNodeTypes", List.of("sms"));
        assertThat(otherUser.preferenceJson()).containsEntry("theme", "system");
        assertThat(otherTenant.preferenceJson()).containsEntry("theme", "system");
    }

    @Test
    void editorPreferenceRejectsUnsupportedPatchKeys() {
        CanvasPreferenceFacade service = new CanvasPreferenceApplicationService();

        assertThatThrownBy(() -> service.upsertEditorPreference(7L, "operator-1", Map.of("unknown", true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unsupported preference key unknown");
    }
}
