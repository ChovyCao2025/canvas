import { describe, expect, it } from 'vitest'
import { canPublishFromChecks, summarizePrePublishChecks } from './prePublishChecks'

describe('prePublishChecks helpers', () => {
  it('blocks publish when any item is ERROR', () => {
    expect(canPublishFromChecks({
      blocking: true,
      items: [{ code: 'NO_ENTRY_NODE', severity: 'ERROR', message: '缺少触发器' }],
    })).toBe(false)
  })

  it('summarizes warnings and errors', () => {
    expect(summarizePrePublishChecks({
      blocking: true,
      items: [
        { code: 'NO_ENTRY_NODE', severity: 'ERROR', message: '缺少触发器' },
        { code: 'NO_TEST_SEND', severity: 'WARNING', message: '尚未测试发送' },
      ],
    })).toEqual({ errors: 1, warnings: 1 })
  })
})
