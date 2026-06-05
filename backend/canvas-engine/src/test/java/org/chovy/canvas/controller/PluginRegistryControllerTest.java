package org.chovy.canvas.web;

import org.chovy.canvas.engine.plugin.PluginRegistryService;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PluginRegistryControllerTest {

    @Test
    void catalogReturnsGroupedPlugins() {
        PluginRegistryService service = mock(PluginRegistryService.class);
        when(service.groupedCatalog()).thenReturn(Map.of("DATA_EXPORTER", List.of(
                new PluginRegistryService.Plugin("csv-export", "DATA_EXPORTER", "CSV Export", true, Map.of()))));
        PluginRegistryController controller = new PluginRegistryController(service);

        StepVerifier.create(controller.catalog())
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData().get("DATA_EXPORTER")).hasSize(1);
                })
                .verifyComplete();
    }

    @Test
    void setEnabledDelegatesWithCurrentVersionHeader() {
        PluginRegistryService service = mock(PluginRegistryService.class);
        PluginRegistryController controller = new PluginRegistryController(service);

        StepVerifier.create(controller.setEnabled("csv-export", "1.2.0", new PluginRegistryController.EnableRequest(true)))
                .assertNext(response -> assertThat(response.getCode()).isEqualTo(0))
                .verifyComplete();

        verify(service).setEnabled("csv-export", true, "1.2.0");
    }
}
