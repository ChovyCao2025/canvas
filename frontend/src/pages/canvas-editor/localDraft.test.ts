/**
 * 测试职责：验证画布本地草稿的读写、清理和与服务端详情的差异比较。
 *
 * 维护说明：localStorage 结构升级时，应保留坏数据安全降级和版本比较场景。
 */
import { beforeEach, describe, expect, it } from 'vitest'
import type { CanvasDetail } from '../../types'
import type { CanvasSettingsLike } from './settingsPresentation'
import {
  clearCanvasLocalDraft,
  isLocalDraftDifferentFromServer,
  readCanvasLocalDraft,
  writeCanvasLocalDraft,
} from './localDraft'

/** 测试用内存版 Storage，避免直接依赖真实浏览器 localStorage。 */
class MemoryStorage implements Storage {
  private readonly data = new Map<string, string>()

  get length() {
    return this.data.size
  }

  clear() {
    this.data.clear()
  }

  getItem(key: string) {
    return this.data.get(key) ?? null
  }

  key(index: number) {
    return Array.from(this.data.keys())[index] ?? null
  }

  removeItem(key: string) {
    this.data.delete(key)
  }

  setItem(key: string, value: string) {
    this.data.set(key, value)
  }
}

/** 构造画布详情样本，供本地草稿差异比较测试复用。 */
function detail(overrides: Partial<CanvasDetail> = {}): CanvasDetail {
  return {
    canvas: {
      id: 1,
      name: '画布',
      status: 0,
      createdAt: '2026-05-24T00:00:00',
      updatedAt: '2026-05-24T00:00:00',
      triggerType: 'REALTIME',
      editVersion: 2,
      ...overrides.canvas,
    },
    graphJson: '{"nodes":[]}',
    draftVersionId: 10,
    ...overrides,
  }
}

describe('canvas editor local draft', () => {
  const settings: CanvasSettingsLike = { triggerType: 'REALTIME' }

  beforeEach(() => {
    Object.defineProperty(globalThis, 'localStorage', {
      value: new MemoryStorage(),
      configurable: true,
    })
  })

  it('writes and reads a canvas local draft', () => {
    writeCanvasLocalDraft({
      canvasId: 1,
      name: '本地画布',
      graphJson: '{"nodes":[{"id":"api"}]}',
      settings,
      editVersion: 3,
      draftVersionId: 10,
      savedAt: 1000,
    })

    expect(readCanvasLocalDraft(1)).toMatchObject({
      canvasId: 1,
      name: '本地画布',
      graphJson: '{"nodes":[{"id":"api"}]}',
      editVersion: 3,
    })
  })

  it('detects a local graph that differs from the server draft', () => {
    const localDraft = {
      canvasId: 1,
      name: '画布',
      graphJson: '{"nodes":[{"id":"api"}]}',
      settings,
      editVersion: 3,
      draftVersionId: 10,
      savedAt: 1000,
    }

    expect(isLocalDraftDifferentFromServer(localDraft, detail())).toBe(true)
  })

  it('clears a saved local draft', () => {
    writeCanvasLocalDraft({
      canvasId: 1,
      name: '本地画布',
      graphJson: '{"nodes":[]}',
      settings,
      editVersion: 3,
      savedAt: 1000,
    })

    clearCanvasLocalDraft(1)

    expect(readCanvasLocalDraft(1)).toBeNull()
  })
})
