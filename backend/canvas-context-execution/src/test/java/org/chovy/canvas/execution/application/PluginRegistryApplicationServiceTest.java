package org.chovy.canvas.execution.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.chovy.canvas.execution.domain.PluginRegistryCatalog.Plugin;
import org.junit.jupiter.api.Test;

class PluginRegistryApplicationServiceTest {

    @Test
    void groupsCatalogByExtensionPointWithPluginKeyOrdering() {
        PluginRegistryApplicationService service = new PluginRegistryApplicationService();

        var catalog = service.groupedCatalog();

        assertThat(catalog.keySet()).containsExactly("action", "condition");
        assertThat(catalog.get("action")).extracting(Plugin::pluginKey)
                .containsExactly("canvas-plugin-approval", "canvas-plugin-message");
        assertThat(catalog.get("condition")).extracting(Plugin::pluginKey)
                .containsExactly("canvas-plugin-risk");
        assertThat(catalog.get("action")).filteredOn(plugin -> plugin.pluginKey().equals("canvas-plugin-message"))
                .singleElement()
                .satisfies(plugin -> assertThat(plugin.compatibility())
                        .containsEntry("minCanvasVersion", "1.0.0"));
    }

    @Test
    void setEnabledNormalizesKeyAndEnforcesVersionCompatibility() {
        PluginRegistryApplicationService service = new PluginRegistryApplicationService();

        service.setEnabled("  CANVAS-PLUGIN-MESSAGE  ", false, "1.0.0");

        assertThat(service.groupedCatalog().get("action"))
                .filteredOn(plugin -> plugin.pluginKey().equals("canvas-plugin-message"))
                .singleElement()
                .extracting(Plugin::enabled)
                .isEqualTo(false);

        assertThatThrownBy(() -> service.setEnabled("canvas-plugin-approval", true, "0.9.0"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("plugin canvas-plugin-approval requires canvas version 1.1.0");
        assertThatThrownBy(() -> service.setEnabled("../bad", true, "1.0.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid plugin key: ../bad");
        assertThatThrownBy(() -> service.setEnabled("missing-plugin", true, "1.0.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plugin missing-plugin does not exist");
    }
}
