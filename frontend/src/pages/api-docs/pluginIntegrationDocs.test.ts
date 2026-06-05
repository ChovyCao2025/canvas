import { describe, expect, it } from 'vitest'

import {
  formatPluginCompatibility,
  pluginStatusText,
  sortPluginCatalog,
  type PluginCatalog,
} from './pluginIntegrationDocs'

describe('pluginIntegrationDocs', () => {
  it('sorts extension groups and plugins for API docs display', () => {
    const catalog: PluginCatalog = {
      DATA_EXPORTER: [
        {
          pluginKey: 'csv-export',
          extensionPoint: 'DATA_EXPORTER',
          displayName: 'CSV Export',
          enabled: true,
          compatibility: { minCanvasVersion: '1.0.0' },
        },
      ],
      CHANNEL_ADAPTER: [
        {
          pluginKey: 'wecom-channel',
          extensionPoint: 'CHANNEL_ADAPTER',
          displayName: 'WeCom',
          enabled: false,
          compatibility: { minCanvasVersion: '1.0.0' },
        },
      ],
    }

    expect(sortPluginCatalog(catalog).map(group => group.extensionPoint)).toEqual([
      'CHANNEL_ADAPTER',
      'DATA_EXPORTER',
    ])
  })

  it('sorts plugins within each extension point', () => {
    const catalog: PluginCatalog = {
      DATA_EXPORTER: [
        {
          pluginKey: 'z-export',
          extensionPoint: 'DATA_EXPORTER',
          displayName: 'Z Export',
          enabled: true,
          compatibility: {},
        },
        {
          pluginKey: 'csv-export',
          extensionPoint: 'DATA_EXPORTER',
          displayName: 'CSV Export',
          enabled: true,
          compatibility: {},
        },
      ],
    }

    expect(sortPluginCatalog(catalog)[0].plugins.map(plugin => plugin.pluginKey)).toEqual([
      'csv-export',
      'z-export',
    ])
  })

  it('formats status and compatibility copy', () => {
    expect(pluginStatusText(true)).toBe('Enabled')
    expect(pluginStatusText(false)).toBe('Disabled')
    expect(formatPluginCompatibility({ minCanvasVersion: '1.2.0' })).toBe('Requires Canvas 1.2.0 or newer')
  })
})
