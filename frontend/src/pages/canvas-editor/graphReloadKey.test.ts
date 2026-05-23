import { describe, expect, it } from 'vitest'
import type { CanvasDetail } from '../../types'
import { getCanvasGraphReloadKey } from './graphReloadKey'

const detail = (overrides: Partial<CanvasDetail> = {}): CanvasDetail => ({
  canvas: {
    id: 1,
    name: '测试画布',
    status: 0,
    createdAt: '2026-05-23T00:00:00',
    updatedAt: '2026-05-23T00:00:00',
    editVersion: 3,
  },
  graphJson: '{"nodes":[{"id":"old"}]}',
  draftVersionId: 10,
  ...overrides,
})

describe('getCanvasGraphReloadKey', () => {
  it('does not change when only canvas status changes after publish', () => {
    const before = detail()
    const afterPublish = detail({
      canvas: { ...before.canvas, status: 1, publishedVersionId: 20 },
    })

    expect(getCanvasGraphReloadKey(afterPublish)).toBe(getCanvasGraphReloadKey(before))
  })

  it('changes when the loaded graph changes', () => {
    expect(getCanvasGraphReloadKey(detail({ graphJson: '{"nodes":[{"id":"new"}]}' })))
      .not.toBe(getCanvasGraphReloadKey(detail()))
  })
})
