package org.chovy.canvas.execution.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.execution.api.PluginRegistryFacade;
import org.chovy.canvas.execution.domain.PluginRegistryCatalog;
import org.springframework.stereotype.Service;

/**
 * 定义 PluginRegistryApplicationService 的执行上下文数据结构或业务契约。
 */
@Service
public class PluginRegistryApplicationService implements PluginRegistryFacade {

    /**
     * 保存 catalog 对应的状态或配置。
     */
    private final PluginRegistryCatalog catalog;

    /**
     * 执行 PluginRegistryApplicationService 对应的业务处理。
     */
    public PluginRegistryApplicationService() {
        this(new PluginRegistryCatalog());
    }

    /**
     * 执行 PluginRegistryApplicationService 对应的业务处理。
     * @param catalog catalog 参数
     */
    public PluginRegistryApplicationService(PluginRegistryCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 执行 groupedCatalog 对应的业务处理。
     * @return 处理后的结果
     */
    @Override
    public Map<String, List<PluginRegistryCatalog.Plugin>> groupedCatalog() {
        return catalog.groupedCatalog();
    }

    /**
     * 执行 setEnabled 对应的业务处理。
     * @param pluginKey pluginKey 参数
     * @param enabled enabled 参数
     * @param canvasVersion canvasVersion 参数
     */
    @Override
    public void setEnabled(String pluginKey, boolean enabled, String canvasVersion) {
        catalog.setEnabled(pluginKey, enabled, canvasVersion);
    }
}
