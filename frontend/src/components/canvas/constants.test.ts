import { describe, expect, it } from 'vitest'

import { DEFAULT_NAMES, PUBLISH_TRIGGER_NODE_TYPES } from './constants'

describe('canvas node display constants', () => {
  it('labels DIRECT_CALL as the API entry node shown to users', () => {
    expect(DEFAULT_NAMES.DIRECT_CALL).toBe('API入口')
  })

  it('uses DIRECT_CALL, not API_TRIGGER, as the publishable API entry', () => {
    expect(PUBLISH_TRIGGER_NODE_TYPES.has('DIRECT_CALL')).toBe(true)
    expect(PUBLISH_TRIGGER_NODE_TYPES.has('API_TRIGGER')).toBe(false)
  })
})
