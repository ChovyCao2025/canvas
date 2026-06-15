package org.chovy.canvas.execution.api;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.execution.domain.PluginRegistryCatalog;

public interface PluginRegistryFacade {

    Map<String, List<PluginRegistryCatalog.Plugin>> groupedCatalog();

    void setEnabled(String pluginKey, boolean enabled, String canvasVersion);
}
