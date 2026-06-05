import { describe, expect, it } from 'vitest'
import { ApiBusinessError } from '../../services/api'
import type { CanvasVersion } from '../../types'
import {
  editorApiErrorMessage,
  extractCanvasVersions,
  extractDryRunExecutionId,
  isApiConflict,
  parseTestRunPayload,
} from './workflowApiAdapters'

describe('workflow API adapters', () => {
  it('normalizes paged and legacy version-list responses', () => {
    const version = canvasVersion(1)

    expect(extractCanvasVersions({ total: 1, list: [version] })).toEqual([version])
    expect(extractCanvasVersions([version])).toEqual([version])
    expect(extractCanvasVersions(undefined)).toEqual([])
  })

  it('extracts a non-empty dry-run execution id', () => {
    expect(extractDryRunExecutionId({ executionId: 'exec-123', result: true })).toBe('exec-123')
    expect(extractDryRunExecutionId({ executionId: '   ' })).toBeUndefined()
    expect(extractDryRunExecutionId(undefined)).toBeUndefined()
  })

  it('parses test-run payloads as JSON objects', () => {
    expect(parseTestRunPayload('{"userId":"u1","score":3}')).toEqual({
      ok: true,
      payload: { userId: 'u1', score: 3 },
    })
    expect(parseTestRunPayload('[]')).toEqual({ ok: false, message: 'Payload 必须是 JSON 对象' })
    expect(parseTestRunPayload('{')).toEqual({ ok: false, message: 'Payload 不是合法 JSON' })
  })

  it('keeps API messages and classifies conflicts without catch casts', () => {
    expect(editorApiErrorMessage(new ApiBusinessError(40001, '名称重复'), '名称保存失败')).toBe('名称重复')
    expect(editorApiErrorMessage(new Error(), '名称保存失败')).toBe('名称保存失败')
    expect(isApiConflict({ response: { status: 409, data: { message: '版本冲突' } } })).toBe(true)
  })
})

function canvasVersion(id: number): CanvasVersion {
  return {
    id,
    canvasId: 100,
    version: id,
    graphJson: '{"nodes":[]}',
    status: 1,
    createdAt: '2026-06-04T00:00:00',
  }
}
