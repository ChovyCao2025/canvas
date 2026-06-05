package org.chovy.canvas.engine.plugin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PluginRegistryServiceTest {

    @Test
    void migrationCreatesPluginRegistryTableWithoutRuntimeCodeLoadingFields() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V161__plugin_integration_foundations.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS built_in_plugin_registry")
                .contains("plugin_key VARCHAR(128) NOT NULL")
                .contains("extension_point VARCHAR(64) NOT NULL")
                .contains("compatibility_json JSON NOT NULL")
                .contains("enabled TINYINT NOT NULL DEFAULT 0")
                .doesNotContain("jar_url")
                .doesNotContain("classloader");
    }

    @Test
    void listReturnsBuiltInPluginsGroupedByExtensionPoint() {
        PluginRegistryService.PluginRepository repository = mock(PluginRegistryService.PluginRepository.class);
        PluginRegistryService service = new PluginRegistryService(repository);
        when(repository.list()).thenReturn(List.of(
                new PluginRegistryService.Plugin("wecom-channel", "CHANNEL_ADAPTER", "WeCom", true, Map.of("minCanvasVersion", "1.0.0")),
                new PluginRegistryService.Plugin("csv-export", "DATA_EXPORTER", "CSV Export", false, Map.of("minCanvasVersion", "1.0.0"))));

        Map<String, List<PluginRegistryService.Plugin>> grouped = service.groupedCatalog();

        assertThat(grouped).containsKeys("CHANNEL_ADAPTER", "DATA_EXPORTER");
        assertThat(grouped.get("CHANNEL_ADAPTER"))
                .extracting(PluginRegistryService.Plugin::pluginKey)
                .containsExactly("wecom-channel");
    }

    @Test
    void enableRejectsIncompatiblePluginBeforePersisting() {
        PluginRegistryService.PluginRepository repository = mock(PluginRegistryService.PluginRepository.class);
        PluginRegistryService service = new PluginRegistryService(repository);
        when(repository.get("ai-gateway")).thenReturn(new PluginRegistryService.Plugin(
                "ai-gateway", "AI_GATEWAY", "AI Gateway", false, Map.of("minCanvasVersion", "9.9.9")));

        assertThatThrownBy(() -> service.setEnabled("ai-gateway", true, "1.0.0"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("plugin ai-gateway requires canvas version 9.9.9");
        verify(repository, never()).setEnabled(argThat(command -> true));
    }

    @Test
    void enablePersistsCompatiblePlugin() {
        PluginRegistryService.PluginRepository repository = mock(PluginRegistryService.PluginRepository.class);
        PluginRegistryService service = new PluginRegistryService(repository);
        when(repository.get("csv-export")).thenReturn(new PluginRegistryService.Plugin(
                "csv-export", "DATA_EXPORTER", "CSV Export", false, Map.of("minCanvasVersion", "1.0.0")));

        service.setEnabled("csv-export", true, "1.2.0");

        verify(repository).setEnabled(argThat(command ->
                command.pluginKey().equals("csv-export") && command.enabled()));
    }

    @Test
    void rejectsUnknownPluginKeys() {
        PluginRegistryService.PluginRepository repository = mock(PluginRegistryService.PluginRepository.class);
        PluginRegistryService service = new PluginRegistryService(repository);
        when(repository.get("missing-plugin")).thenReturn(null);

        assertThatThrownBy(() -> service.setEnabled("missing-plugin", true, "1.0.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("plugin missing-plugin does not exist");
    }
}
