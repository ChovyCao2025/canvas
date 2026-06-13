import { describe, expect, it } from 'vitest'

import {
  buildPluginRegistry,
  getNodeConfigSchema,
  listConfigurableNodeTypes,
} from './pluginRegistry'
import type { PluginManifestV1 } from './pluginManifest'

const couponPlugin: PluginManifestV1 = {
  id: 'coupon-plugin',
  name: 'Coupon Plugin',
  version: '0.1.0',
  canvasCoreVersion: '>=0.1.0 <1.0.0',
  extensionPoints: ['node-handler'],
  permissions: ['coupon:grant'],
  nodes: ['COUPON_GRANT'],
  templates: ['coupon-release'],
  configSchema: {
    type: 'object',
    properties: {},
  },
  nodeConfigSchemas: {
    COUPON_GRANT: {
      fields: [
        { key: 'couponId', label: 'Coupon', type: 'text', required: true },
        { key: 'sendMessage', label: 'Send message', type: 'boolean', defaultValue: false },
      ],
    },
  },
}

describe('schemaConfigPanel plugin registry helpers', () => {
  it('indexes registered node config schemas by node type', () => {
    const registry = buildPluginRegistry([couponPlugin])

    expect(listConfigurableNodeTypes(registry)).toEqual(['COUPON_GRANT'])
    expect(getNodeConfigSchema(registry, 'COUPON_GRANT')).toEqual(couponPlugin.nodeConfigSchemas?.COUPON_GRANT)
  })

  it('rejects manifests that declare unsupported permissions', () => {
    expect(() =>
      buildPluginRegistry([
        {
          ...couponPlugin,
          permissions: ['coupon:grant', 'admin:root'],
        },
      ]),
    ).toThrow(/Unsupported plugin permission: admin:root/)
  })

  it('rejects duplicate node type declarations across manifests', () => {
    expect(() =>
      buildPluginRegistry([
        couponPlugin,
        {
          ...couponPlugin,
          id: 'coupon-plugin-v2',
          nodes: ['COUPON_GRANT'],
        },
      ]),
    ).toThrow(/Duplicate plugin node type: COUPON_GRANT/)
  })
})
