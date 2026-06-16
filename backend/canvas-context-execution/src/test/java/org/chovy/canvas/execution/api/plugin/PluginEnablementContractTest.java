package org.chovy.canvas.execution.api.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class PluginEnablementContractTest {

    @Test
    void exposesEnablementMetadataWithoutSecondRegistrySurface() {
        List<String> permissions = new ArrayList<>(List.of("message:send"));
        List<String> nodeTypes = new ArrayList<>(List.of("message.send"));
        PluginEnablementView view = new PluginEnablementView(
                "canvas-plugin-message",
                "0.1.0",
                false,
                permissions,
                nodeTypes,
                "disabled by workspace policy");

        permissions.add("mutated");
        nodeTypes.clear();

        assertThat(view.enabled()).isFalse();
        assertThat(view.permissions()).containsExactly("message:send");
        assertThat(view.nodeTypes()).containsExactly("message.send");
        assertThat(view.disabledReason()).contains("workspace");
        assertThat(PluginEnablementView.class.getSimpleName()).doesNotContain("Registry");
        assertThatThrownBy(() -> view.permissions().add("other"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
