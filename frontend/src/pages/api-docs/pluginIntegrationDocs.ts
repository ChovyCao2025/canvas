/**
 * 页面职责：插件集成 API 文档展示辅助函数。
 *
 * 维护说明：只处理内置插件目录的排序和文案，不参与插件启停业务决策。
 */
export interface PluginItem {
  /** 内置插件稳定 key。 */
  pluginKey: string

  /** 插件扩展点，例如 CHANNEL_ADAPTER。 */
  extensionPoint: string

  /** 面向运营展示的名称。 */
  displayName: string

  /** 当前启用状态。 */
  enabled: boolean

  /** 兼容性约束，当前至少包含 minCanvasVersion。 */
  compatibility: Record<string, unknown>

  /** 可配置项 schema，仅用于展示或配置面板后续接入。 */
  configSchema?: Record<string, unknown>
}

export type PluginCatalog = Record<string, PluginItem[]>

export interface PluginCatalogGroup {
  extensionPoint: string
  plugins: PluginItem[]
}

export function sortPluginCatalog(catalog: PluginCatalog): PluginCatalogGroup[] {
  return Object.entries(catalog)
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([extensionPoint, plugins]) => ({
      extensionPoint,
      plugins: [...plugins].sort((left, right) => left.pluginKey.localeCompare(right.pluginKey)),
    }))
}

export function pluginStatusText(enabled: boolean) {
  return enabled ? 'Enabled' : 'Disabled'
}

export function formatPluginCompatibility(compatibility: Record<string, unknown>) {
  return `Requires Canvas ${String(compatibility.minCanvasVersion ?? '1.0.0')} or newer`
}
