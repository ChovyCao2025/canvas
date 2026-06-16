package org.chovy.canvas.execution.api;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.execution.domain.PluginRegistryCatalog;

/**
 * 定义 PluginRegistryFacade 的执行上下文数据结构或业务契约。
 */
public interface PluginRegistryFacade {

    /**
     * 执行 groupedCatalog 对应的业务处理。
     * @return 处理后的结果
     */
    Map<String, List<PluginRegistryCatalog.Plugin>> groupedCatalog();

    /**
     * 执行 setEnabled 对应的业务处理。
     * @param pluginKey pluginKey 参数
     * @param enabled enabled 参数
     * @param canvasVersion canvasVersion 参数
     */
    void setEnabled(String pluginKey, boolean enabled, String canvasVersion);
}
