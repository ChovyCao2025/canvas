import { describe, expect, it } from 'vitest'
import { buildMessagePreviewPayload } from './messagePreview'

describe('message preview payload', () => {
  it('includes graph json, node id, user id, and parsed context', () => {
    expect(buildMessagePreviewPayload({
      canvasId: 62,
      nodeId: 'send',
      userId: 'u1',
      graphJson: '{"nodes":[]}',
      contextJson: '{"phone":"13812345678"}',
    })).toEqual({
      canvasId: 62,
      nodeId: 'send',
      userId: 'u1',
      graphJson: '{"nodes":[]}',
      context: { phone: '13812345678' },
    })
  })

  it('uses an empty context when the context editor is blank', () => {
    expect(buildMessagePreviewPayload({
      canvasId: 62,
      nodeId: 'send',
      userId: 'u1',
      graphJson: '{"nodes":[]}',
      contextJson: '  ',
    }).context).toEqual({})
  })
})
