package org.chovy.canvas.execution.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.execution.api.PluginRegistryFacade;
import org.chovy.canvas.execution.domain.PluginRegistryCatalog;
import org.springframework.stereotype.Service;

@Service
public class PluginRegistryApplicationService implements PluginRegistryFacade {

    private final PluginRegistryCatalog catalog;

    public PluginRegistryApplicationService() {
        this(new PluginRegistryCatalog());
    }

    public PluginRegistryApplicationService(PluginRegistryCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Map<String, List<PluginRegistryCatalog.Plugin>> groupedCatalog() {
        return catalog.groupedCatalog();
    }

    @Override
    public void setEnabled(String pluginKey, boolean enabled, String canvasVersion) {
        catalog.setEnabled(pluginKey, enabled, canvasVersion);
    }
}
