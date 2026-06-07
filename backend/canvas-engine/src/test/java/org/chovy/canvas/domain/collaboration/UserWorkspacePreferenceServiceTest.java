package org.chovy.canvas.domain.collaboration;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserWorkspacePreferenceServiceTest {

    @Test
    void migrationCreatesTenantScopedPreferenceTable() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V263__collaboration_personalization_reporting.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS user_workspace_preference")
                .contains("tenant_id BIGINT NOT NULL")
                .contains("user_id VARCHAR(128) NOT NULL")
                .contains("preference_key VARCHAR(128) NOT NULL")
                .contains("preference_json JSON NOT NULL")
                .contains("UNIQUE KEY uk_user_workspace_preference");
    }

    @Test
    void getEditorPreferenceReturnsStoredValueForTenantAndUser() {
        UserWorkspacePreferenceService.PreferenceRepository repository =
                mock(UserWorkspacePreferenceService.PreferenceRepository.class);
        UserWorkspacePreferenceService service = new UserWorkspacePreferenceService(repository);
        when(repository.find(8L, "operator-1", "canvas-editor")).thenReturn(Optional.of(
                new UserWorkspacePreferenceService.Preference(
                        "canvas-editor",
                        Map.of("theme", "dark", "sidebarCollapsed", true))));

        UserWorkspacePreferenceService.Preference result = service.getEditorPreference(8L, "operator-1");

        assertThat(result.preferenceKey()).isEqualTo("canvas-editor");
        assertThat(result.preferenceJson()).containsEntry("theme", "dark");
        assertThat(result.preferenceJson()).containsEntry("sidebarCollapsed", true);
    }

    @Test
    void getEditorPreferenceReturnsDefaultsWhenMissing() {
        UserWorkspacePreferenceService.PreferenceRepository repository =
                mock(UserWorkspacePreferenceService.PreferenceRepository.class);
        UserWorkspacePreferenceService service = new UserWorkspacePreferenceService(repository);
        when(repository.find(8L, "operator-1", "canvas-editor")).thenReturn(Optional.empty());

        UserWorkspacePreferenceService.Preference result = service.getEditorPreference(8L, "operator-1");

        assertThat(result.preferenceJson()).containsEntry("theme", "system");
        assertThat(result.preferenceJson()).containsEntry("sidebarCollapsed", false);
        assertThat(result.preferenceJson()).containsEntry("notificationLevel", "mentions");
    }

    @Test
    void upsertEditorPreferenceRejectsUnknownKeysAndPersistsAllowedKeys() {
        UserWorkspacePreferenceService.PreferenceRepository repository =
                mock(UserWorkspacePreferenceService.PreferenceRepository.class);
        UserWorkspacePreferenceService service = new UserWorkspacePreferenceService(repository);

        assertThatThrownBy(() -> service.upsertEditorPreference(8L, "operator-1", Map.of("unsafe", true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported preference key unsafe");

        service.upsertEditorPreference(8L, "operator-1",
                Map.of("theme", "dark", "recentNodeTypes", List.of("SEND_MESSAGE")));

        verify(repository).upsert(argThat(saved ->
                saved.tenantId().equals(8L)
                        && saved.userId().equals("operator-1")
                        && saved.preferenceKey().equals("canvas-editor")
                        && saved.preferenceJson().containsKey("recentNodeTypes")));
    }
}
