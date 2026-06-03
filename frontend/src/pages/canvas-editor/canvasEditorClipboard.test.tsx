/**
 * 测试职责：验证画布节点粘贴时不会共享源节点 bizConfig 引用。
 *
 * 维护说明：复制/粘贴会移除旧连线目标，但必须保留业务配置自身的深层结构。
 */
import { describe, expect, it } from 'vitest'
import { cloneCanvasNodeBizConfigForPaste } from './canvasEditorClipboard'

describe('canvas editor clipboard helpers', () => {
  it('deep-clones pasted node config and clears copied edge targets', () => {
    const source = {
      nextNodeId: 'existing-node',
      nested: {
        rules: [{ field: 'city', value: 'SG' }],
      },
      branches: [
        { label: '命中', nextNodeId: 'existing-node' },
        { label: '继续', nextNodeId: 'fresh-node' },
      ],
    }

    const cloned = cloneCanvasNodeBizConfigForPaste(source, new Set(['existing-node']))

    expect(cloned).not.toBe(source)
    expect(cloned.nested).not.toBe(source.nested)
    expect(cloned.branches).not.toBe(source.branches)
    expect(cloned.nextNodeId).toBeUndefined()
    expect(cloned.branches).toEqual([
      { label: '命中', nextNodeId: undefined },
      { label: '继续', nextNodeId: 'fresh-node' },
    ])

    ;((cloned.nested as { rules: Array<{ value: string }> }).rules[0]).value = 'HK'
    expect((source.nested.rules[0]).value).toBe('SG')
  })
})
