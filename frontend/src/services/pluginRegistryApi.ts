/**
 * 服务职责：内置插件注册表 API 封装。
 *
 * 维护说明：当前只暴露内置插件元数据和启用状态，不加载第三方运行时代码。
 */
import type { R } from '../types'
import type { PluginCatalog } from '../pages/api-docs/pluginIntegrationDocs'
import http from './api'

export const pluginRegistryApi = {
  /** 查询内置插件目录，后端按扩展点分组返回。 */
  catalog: () =>
    http.get<R<PluginCatalog>, R<PluginCatalog>>('/canvas/plugins'),

  /** 更新内置插件启用状态，兼容性检查由后端执行。 */
  setEnabled: (pluginKey: string, enabled: boolean, canvasVersion = '1.0.0') =>
    http.put<R<void>, R<void>>(
      `/canvas/plugins/${pluginKey}/enabled`,
      { enabled },
      { headers: { 'X-Canvas-Version': canvasVersion } },
    ),
}
